package org.moera.node.operations;

import java.lang.reflect.Constructor;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.IncorrectSignatureException;
import org.moera.node.data.Comment;
import org.moera.node.data.Entry;
import org.moera.node.data.Posting;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionRepository;
import org.moera.node.event.EventManager;
import org.moera.node.fingerprint.FingerprintManager;
import org.moera.node.fingerprint.FingerprintObjectType;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionInfo;
import org.moera.node.model.ReactionsSliceInfo;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.event.CommentReactionsChangedEvent;
import org.moera.node.model.event.PostingReactionsChangedEvent;
import org.moera.node.model.notification.PostingReactionsUpdatedNotification;
import org.moera.node.naming.NamingCache;
import org.moera.node.notification.send.Directions;
import org.moera.node.notification.send.NotificationSenderPool;
import org.moera.node.util.EmojiList;
import org.moera.node.util.MomentFinder;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

@Component
public class ReactionOperations {

    public static final Duration UNSIGNED_TTL = Duration.of(15, ChronoUnit.MINUTES);
    public static final int MAX_REACTIONS_PER_REQUEST = 200;

    @Inject
    private RequestContext requestContext;

    @Inject
    private ReactionRepository reactionRepository;

    @Inject
    private NamingCache namingCache;

    @Inject
    private FingerprintManager fingerprintManager;

    @Inject
    private EventManager eventManager;

    @Inject
    private NotificationSenderPool notificationSenderPool;

    @Inject
    private ReactionTotalOperations reactionTotalOperations;

    @Inject
    private PlatformTransactionManager txManager;

    private final MomentFinder momentFinder = new MomentFinder();

    public void validate(ReactionDescription reactionDescription, Entry entry) throws AuthenticationException {
        if (reactionDescription.getSignature() == null) {
            String ownerName = requestContext.getClientName();
            if (StringUtils.isEmpty(ownerName)) {
                throw new AuthenticationException();
            }
            if (!StringUtils.isEmpty(reactionDescription.getOwnerName())
                    && !reactionDescription.getOwnerName().equals(ownerName)) {
                throw new AuthenticationException();
            }
            reactionDescription.setOwnerName(ownerName);
        } else {
            byte[] signingKey = namingCache.get(reactionDescription.getOwnerName()).getSigningKey();
            Constructor<? extends Fingerprint> constructor = fingerprintManager.getConstructor(
                    FingerprintObjectType.REACTION, reactionDescription.getSignatureVersion(),
                    ReactionDescription.class, byte[].class);
            if (!CryptoUtil.verify(
                    reactionDescription.getSignature(),
                    signingKey,
                    constructor,
                    reactionDescription,
                    entry.getCurrentRevision().getDigest())) {
                throw new IncorrectSignatureException();
            }
        }

        EmojiList accepted = new EmojiList(!reactionDescription.isNegative()
                ? entry.getAcceptedReactionsPositive()
                : entry.getAcceptedReactionsNegative());
        if (!accepted.isAccepted(reactionDescription.getEmoji())) {
            throw new ValidationFailure("reaction.not-accepted");
        }
    }

    public Reaction post(ReactionDescription reactionDescription, Entry entry, Consumer<Reaction> reactionDeleted,
                         Consumer<Reaction> reactionAdded) {
        Reaction reaction = reactionRepository.findByEntryIdAndOwner(entry.getId(), reactionDescription.getOwnerName());
        if (reaction == null || reaction.getDeadline() == null
                || reaction.isNegative() != reactionDescription.isNegative()
                || reaction.getEmoji() != reactionDescription.getEmoji()
                || reaction.getSignature() == null && reactionDescription.getSignature() != null) {

            if (reaction != null) {
                reactionTotalOperations.changeTotals(entry, reaction, -1);
                reaction.setDeletedAt(Util.now());
                Duration reactionTtl = requestContext.getOptions().getDuration("reaction.deleted.lifetime");
                reaction.setDeadline(Timestamp.from(Instant.now().plus(reactionTtl)));
                if (reactionDeleted != null) {
                    reactionDeleted.accept(reaction);
                }
            }

            reaction = new Reaction();
            reaction.setId(UUID.randomUUID());
            reaction.setEntryRevision(entry.getCurrentRevision());
            reactionDescription.toReaction(reaction);
            if (reactionDescription.getSignature() == null) {
                reaction.setDeadline(Timestamp.from(Instant.now().plus(ReactionOperations.UNSIGNED_TTL)));
            }
            reaction.setMoment(momentFinder.find(
                    moment -> reactionRepository.countMoments(entry.getId(), moment) == 0,
                    Util.now()));
            reaction = reactionRepository.save(reaction);
            entry.getCurrentRevision().addReaction(reaction);

            reactionTotalOperations.changeTotals(entry, reaction, 1);
            if (reaction.getSignature() != null && reactionAdded != null) {
                reactionAdded.accept(reaction);
            }
        }
        reactionRepository.flush();

        return reaction;
    }

    public ReactionsSliceInfo getBefore(UUID entryId, boolean negative, Integer emoji, long before, int limit) {
        Pageable pageable = PageRequest.of(0, limit + 1, Sort.Direction.DESC, "moment");
        Page<Reaction> page = emoji == null
                ? reactionRepository.findSlice(entryId, negative, Long.MIN_VALUE, before, pageable)
                : reactionRepository.findSliceWithEmoji(entryId, negative, emoji, Long.MIN_VALUE, before, pageable);
        ReactionsSliceInfo sliceInfo = new ReactionsSliceInfo();
        sliceInfo.setBefore(before);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setAfter(Long.MIN_VALUE);
        } else {
            sliceInfo.setAfter(page.getContent().get(limit).getMoment());
        }
        sliceInfo.setTotal((int) page.getTotalElements());
        sliceInfo.setReactions(page.stream().map(ReactionInfo::new).collect(Collectors.toList()));
        return sliceInfo;
    }

    public void delete(String ownerName, Entry entry, Consumer<Reaction> reactionDeleted) {
        Reaction reaction = reactionRepository.findByEntryIdAndOwner(entry.getId(), ownerName);
        if (reaction != null) {
            reactionTotalOperations.changeTotals(entry, reaction, -1);
            reaction.setDeletedAt(Util.now());
            Duration reactionTtl = requestContext.getOptions().getDuration("reaction.deleted.lifetime");
            reaction.setDeadline(Timestamp.from(Instant.now().plus(reactionTtl)));
            if (reactionDeleted != null) {
                reactionDeleted.accept(reaction);
            }
        }
        reactionRepository.flush();
    }

    @Scheduled(fixedDelayString = "PT15M")
    public void purgeExpired() throws Throwable {
        Set<Entry> changed = new HashSet<>();
        Transaction.execute(txManager, () -> {
            reactionRepository.findExpired(Util.now()).forEach(reaction -> {
                Entry entry = reaction.getEntryRevision().getEntry();
                if (reaction.getDeletedAt() == null) {
                    List<Reaction> deleted = reactionRepository.findDeletedByEntryIdAndOwner(
                            entry.getId(),
                            reaction.getOwnerName(),
                            PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "deletedAt")));
                    if (deleted.size() > 0) {
                        Reaction next = deleted.get(0);
                        next.setDeletedAt(null);
                        if (next.getSignature() != null) {
                            next.setDeadline(null);
                        }
                        reactionTotalOperations.changeTotals(entry, next, 1);
                    }
                    reactionTotalOperations.changeTotals(entry, reaction, -1);
                    changed.add(entry);
                }
                reactionRepository.delete(reaction);
            });
            return null;
        });
        for (Entry entry : changed) {
            switch (entry.getEntryType()) {
                case POSTING: {
                    Posting posting = (Posting) entry;
                    eventManager.send(posting.getNodeId(), new PostingReactionsChangedEvent(posting));
                    var totalsInfo = reactionTotalOperations.getInfo(posting);
                    notificationSenderPool.send(Directions.postingSubscribers(posting.getId()),
                            new PostingReactionsUpdatedNotification(posting.getId(), totalsInfo.getPublicInfo()));
                    break;
                }

                case COMMENT: {
                    Comment comment = (Comment) entry;
                    eventManager.send(comment.getNodeId(), new CommentReactionsChangedEvent(comment));
                    break;
                }
            }
        }
    }

}
