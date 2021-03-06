package org.moera.node.instant;

import java.util.UUID;

import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriptionType;
import org.moera.node.global.RequestContext;
import org.moera.node.model.event.StoryAddedEvent;
import org.moera.node.model.event.StoryDeletedEvent;
import org.moera.node.operations.StoryOperations;
import org.springframework.stereotype.Component;

@Component
public class SubscriberInstants {

    @Inject
    private RequestContext requestContext;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private InstantOperations instantOperations;

    public void added(Subscriber subscriber) {
        if (subscriber.getSubscriptionType() != SubscriptionType.FEED) {
            return;
        }

        Story story = findDeletedStory(subscriber.getRemoteNodeName());
        if (story != null && !story.isRead()) {
            storyRepository.delete(story);
            requestContext.send(new StoryDeletedEvent(story, true));
        }

        story = new Story(UUID.randomUUID(), requestContext.nodeId(), StoryType.SUBSCRIBER_ADDED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(subscriber.getRemoteNodeName());
        story.setSummary(buildAddedSummary(subscriber));
        storyOperations.updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        requestContext.send(new StoryAddedEvent(story, true));
        instantOperations.feedStatusUpdated();
    }

    private static String buildAddedSummary(Subscriber subscriber) {
        return String.format("%s subscribed to your %s",
                InstantUtil.formatNodeName(subscriber.getRemoteNodeName()),
                Feed.getStandard(subscriber.getFeedName()).getTitle());
    }

    public void deleted(Subscriber subscriber) {
        if (subscriber.getSubscriptionType() != SubscriptionType.FEED) {
            return;
        }

        Story story = findAddedStory(subscriber.getRemoteNodeName());
        if (story != null && !story.isRead()) {
            storyRepository.delete(story);
            requestContext.send(new StoryDeletedEvent(story, true));
        }

        story = new Story(UUID.randomUUID(), requestContext.nodeId(), StoryType.SUBSCRIBER_DELETED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(subscriber.getRemoteNodeName());
        story.setSummary(buildDeletedSummary(subscriber));
        storyOperations.updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        requestContext.send(new StoryAddedEvent(story, true));
        instantOperations.feedStatusUpdated();
    }

    private static String buildDeletedSummary(Subscriber subscriber) {
        return String.format("%s unsubscribed from your %s",
                InstantUtil.formatNodeName(subscriber.getRemoteNodeName()),
                Feed.getStandard(subscriber.getFeedName()).getTitle());
    }

    private Story findAddedStory(String remoteNodeName) {
        return storyRepository.findByRemoteNodeName(requestContext.nodeId(), Feed.INSTANT, StoryType.SUBSCRIBER_ADDED,
                remoteNodeName).stream().findFirst().orElse(null);
    }

    private Story findDeletedStory(String remoteNodeName) {
        return storyRepository.findByRemoteNodeName(requestContext.nodeId(), Feed.INSTANT, StoryType.SUBSCRIBER_DELETED,
                remoteNodeName).stream().findFirst().orElse(null);
    }

}
