package org.moera.node.picker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.api.NodeApiException;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.ReactionTotalRepository;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.StoryAttributes;
import org.moera.node.model.SubscriberDescriptionQ;
import org.moera.node.model.SubscriberInfo;
import org.moera.node.model.event.Event;
import org.moera.node.model.event.PostingAddedEvent;
import org.moera.node.model.event.PostingRestoredEvent;
import org.moera.node.model.event.PostingUpdatedEvent;
import org.moera.node.model.notification.FeedPostingAddedNotification;
import org.moera.node.model.notification.PostingUpdatedNotification;
import org.moera.node.notification.send.DirectedNotification;
import org.moera.node.notification.send.Directions;
import org.moera.node.notification.send.NotificationSenderPool;
import org.moera.node.operations.ReactionTotalOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.task.Task;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Picker extends Task {

    private static Logger log = LoggerFactory.getLogger(Picker.class);

    private String remoteNodeName;
    private BlockingQueue<Pick> queue = new LinkedBlockingQueue<>();
    private boolean stopped = false;
    private PickerPool pool;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private ReactionTotalRepository reactionTotalRepository;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private ReactionTotalOperations reactionTotalOperations;

    @Inject
    private NotificationSenderPool notificationSenderPool;

    public Picker(PickerPool pool, String remoteNodeName) {
        this.pool = pool;
        this.remoteNodeName = remoteNodeName;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void put(@NotNull Pick pick) throws InterruptedException {
        queue.put(pick);
    }

    @Override
    public void run() {
        while (!stopped) {
            Pick pick;
            try {
                pick = queue.poll(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                continue;
            }
            if (pick == null) {
                stopped = true;
                if (!queue.isEmpty()) { // queue may receive content before the previous statement
                    stopped = false;
                }
            } else {
                download(pick);
            }
        }
        pool.deletePicker(nodeId, remoteNodeName);
    }

    private void download(Pick pick) {
        initLoggingDomain();
        log.info("Downloading from node '{}', postingId = {}", remoteNodeName, pick.getRemotePostingId());

        nodeApi.setNodeId(nodeId);

        List<Event> events = new ArrayList<>();
        List<DirectedNotification> notifications = new ArrayList<>();
        inTransaction(
            () -> downloadPosting(pick.getRemotePostingId(), pick.getFeedName(), events, notifications),
            posting -> {
                events.forEach(event -> eventManager.send(nodeId, event));
                notifications.forEach(
                        dn -> notificationSenderPool.send(dn.getDirection().nodeId(nodeId), dn.getNotification()));

                succeeded(posting);
            }
        );
    }

    private Posting downloadPosting(String remotePostingId, String feedName, List<Event> events,
                                    List<DirectedNotification> notifications) throws NodeApiException {
        PostingInfo postingInfo = nodeApi.getPosting(remoteNodeName, remotePostingId);
        Posting posting = postingRepository.findByReceiverId(nodeId, remoteNodeName, remotePostingId).orElse(null);
        if (posting == null) {
            posting = new Posting();
            posting.setId(UUID.randomUUID());
            posting.setNodeId(nodeId);
            posting.setReceiverName(postingInfo.isOriginal() ? remoteNodeName : postingInfo.getReceiverName());
            posting = postingRepository.save(posting);
            postingInfo.toPickedPosting(posting);
            downloadRevisions(posting);
            subscribe(remotePostingId);
            events.add(new PostingAddedEvent(posting));
            notifications.add(new DirectedNotification(
                    Directions.feedSubscribers(feedName),
                    new FeedPostingAddedNotification(feedName, posting.getId())));
            publish(feedName, posting, events);
        } else if (postingInfo.differFromPickedPosting(posting)) {
            postingInfo.toPickedPosting(posting);
            downloadRevisions(posting);
            if (posting.getDeletedAt() == null) {
                events.add(new PostingUpdatedEvent(posting));
            } else {
                posting.setDeletedAt(null);
                events.add(new PostingRestoredEvent(posting));
            }
            notifications.add(new DirectedNotification(
                    Directions.postingSubscribers(posting.getId()),
                    new PostingUpdatedNotification(posting.getId())));
        }
        var reactionTotals = reactionTotalRepository.findAllByEntryId(posting.getId());
        if (!reactionTotalOperations.isSame(reactionTotals, postingInfo.getReactions())) {
            reactionTotalOperations.replaceAll(posting, postingInfo.getReactions());
        }
        return posting;
    }

    private void downloadRevisions(Posting posting) throws NodeApiException {
        PostingRevisionInfo[] revisionInfos = nodeApi.getPostingRevisions(remoteNodeName, posting.getReceiverEntryId());
        EntryRevision currentRevision = null;
        for (PostingRevisionInfo revisionInfo : revisionInfos) {
            if (revisionInfo.getId().equals(posting.getCurrentReceiverRevisionId())) {
                if (revisionInfo.getDeletedAt() == null) {
                    currentRevision = posting.getCurrentRevision();
                } else {
                    posting.getCurrentRevision().setDeletedAt(Util.now());
                    posting.getCurrentRevision().setReceiverDeletedAt(Util.toTimestamp(revisionInfo.getDeletedAt()));
                }
                break;
            }
            EntryRevision revision = new EntryRevision();
            revision.setId(UUID.randomUUID());
            revision.setEntry(posting);
            revision = entryRevisionRepository.save(revision);
            posting.addRevision(revision);
            revisionInfo.toPickedEntryRevision(revision);
            PostingFingerprint fingerprint = new PostingFingerprint(posting, revision);
            revision.setDigest(CryptoUtil.digest(fingerprint));
            if (revisionInfo.getDeletedAt() == null) {
                currentRevision = revision;
            }
            posting.setTotalRevisions(posting.getTotalRevisions() + 1);
        }
        posting.setCurrentRevision(currentRevision);
        if (currentRevision != null) {
            posting.setCurrentReceiverRevisionId(currentRevision.getReceiverRevisionId());
        }
    }

    private void publish(String feedName, Posting posting, List<Event> events) {
        int totalStories = storyRepository.countByFeedAndTypeAndEntryId(nodeId, feedName, StoryType.POSTING_ADDED,
                posting.getId());
        if (totalStories > 0) {
            return;
        }
        StoryAttributes publication = new StoryAttributes();
        publication.setFeedName(feedName);
        storyOperations.publish(posting, Collections.singletonList(publication), nodeId, events::add);
    }

    private void subscribe(String remotePostingId) throws NodeApiException {
        SubscriberInfo subscriberInfo = nodeApi.postSubscriber(remoteNodeName, generateCarte(),
                new SubscriberDescriptionQ(SubscriptionType.POSTING, null, remotePostingId));
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setNodeId(nodeId);
        subscription.setSubscriptionType(SubscriptionType.POSTING);
        subscription.setRemoteSubscriberId(subscriberInfo.getId());
        subscription.setRemoteNodeName(remoteNodeName);
        subscription.setRemoteEntryId(remotePostingId);
        subscriptionRepository.save(subscription);
    }

    private void succeeded(Posting posting) {
        initLoggingDomain();
        log.info("Posting downloaded successfully, id = {}", posting.getId());
    }

    @Override
    protected void error(Throwable e) {
        failed(e.getMessage());
    }

    private void failed(String message) {
        initLoggingDomain();
        log.error(message);
    }

}
