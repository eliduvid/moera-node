package org.moera.node.instant;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.global.RequestContext;
import org.moera.node.model.event.StoryAddedEvent;
import org.moera.node.model.event.StoryDeletedEvent;
import org.moera.node.model.event.StoryUpdatedEvent;
import org.moera.node.operations.StoryOperations;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class CommentReactionInstants {

    private static final Duration GROUP_PERIOD = Duration.of(6, ChronoUnit.HOURS);

    @Inject
    private RequestContext requestContext;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private InstantOperations instantOperations;

    public void added(String nodeName, String postingId, String commentId, String ownerName, String commentHeading,
                      boolean negative, int emoji) {
        if (ownerName.equals(requestContext.nodeName())) {
            return;
        }

        StoryType storyType = negative ? StoryType.COMMENT_REACTION_ADDED_NEGATIVE
                : StoryType.COMMENT_REACTION_ADDED_POSITIVE;

        boolean isNewStory = false;
        Story story = storyRepository.findFullByRemotePostingAndCommentId(
                requestContext.nodeId(), Feed.INSTANT, storyType, nodeName, postingId, commentId).stream()
                .findFirst().orElse(null);
        if (story == null || story.getCreatedAt().toInstant().plus(GROUP_PERIOD).isBefore(Instant.now())) {
            isNewStory = true;
            story = new Story(UUID.randomUUID(), requestContext.nodeId(), storyType);
            story.setFeedName(Feed.INSTANT);
            story.setRemoteNodeName(nodeName);
            story.setRemotePostingId(postingId);
            story.setRemoteCommentId(commentId);
            story.setRemoteHeading(commentHeading);
            story.setMoment(0L);
            story = storyRepository.save(story);
        }

        Story substory = new Story(UUID.randomUUID(), requestContext.nodeId(), storyType);
        story.setRemoteNodeName(nodeName);
        story.setRemotePostingId(postingId);
        story.setRemoteCommentId(commentId);
        substory.setRemoteOwnerName(ownerName);
        substory.setSummary(buildSummary(ownerName, emoji));
        substory.setMoment(0L);
        substory = storyRepository.save(substory);
        story.addSubstory(substory);

        updated(story, isNewStory, true);
        instantOperations.feedStatusUpdated();
    }

    public void deleted(String nodeName, String postingId, String commentId, String ownerName, boolean negative) {
        if (ownerName.equals(requestContext.nodeName())) {
            return;
        }

        StoryType storyType = negative ? StoryType.COMMENT_REACTION_ADDED_NEGATIVE
                : StoryType.COMMENT_REACTION_ADDED_POSITIVE;
        List<Story> stories = storyRepository.findFullByRemotePostingAndCommentId(
                requestContext.nodeId(), Feed.INSTANT, storyType, nodeName, postingId, commentId);
        for (Story story : stories) {
            Story substory = story.getSubstories().stream()
                    .filter(t -> Objects.equals(t.getRemoteOwnerName(), ownerName))
                    .findAny()
                    .orElse(null);
            if (substory != null) {
                story.removeSubstory(substory);
                storyRepository.delete(substory);
                updated(story, false, false);
            }
        }
        instantOperations.feedStatusUpdated();
    }

    public void deletedAll(String nodeName, String postingId, String commentId) {
        storyRepository.deleteByRemotePostingAndCommentId(requestContext.nodeId(), Feed.INSTANT,
                StoryType.COMMENT_REACTION_ADDED_POSITIVE, nodeName, postingId, commentId);
        storyRepository.deleteByRemotePostingAndCommentId(requestContext.nodeId(), Feed.INSTANT,
                StoryType.COMMENT_REACTION_ADDED_NEGATIVE, nodeName, postingId, commentId);
        instantOperations.feedStatusUpdated();
    }

    private void updated(Story story, boolean isNew, boolean isAdded) {
        List<Story> stories = story.getSubstories().stream()
                .sorted(Comparator.comparing(Story::getCreatedAt).reversed())
                .collect(Collectors.toList());
        if (stories.size() == 0) {
            storyRepository.delete(story);
            if (!isNew) {
                requestContext.send(new StoryDeletedEvent(story, true));
            }
            return;
        }

        story.setSummary(buildAddedSummary(story, stories));
        story.setPublishedAt(Util.now());
        if (isAdded) {
            story.setRead(false);
            story.setViewed(false);
        }
        storyOperations.updateMoment(story);
        requestContext.send(isNew ? new StoryAddedEvent(story, true) : new StoryUpdatedEvent(story, true));
    }

    private String buildAddedSummary(Story story, List<Story> stories) {
        StringBuilder buf = new StringBuilder();
        buf.append(stories.get(0).getSummary());
        if (stories.size() > 1) {
            buf.append(stories.size() == 2 ? " and " : ", ");
            buf.append(stories.get(1).getSummary());
        }
        if (stories.size() > 2) {
            buf.append(" and ");
            buf.append(stories.size() - 2);
            buf.append(stories.size() == 3 ? " other" : " others");
        }
        buf.append(story.getStoryType() == StoryType.COMMENT_REACTION_ADDED_POSITIVE ? " supported" : " opposed");
        buf.append(" your comment \"");
        buf.append(Util.he(story.getRemoteHeading()));
        buf.append("\" on ");
        buf.append(Objects.equals(story.getRemoteNodeName(), requestContext.nodeName()) ? "your"
                : InstantUtil.formatNodeName(story.getRemoteNodeName()));
        buf.append(" post");
        return buf.toString();
    }

    private static String buildSummary(String ownerName, int emoji) {
        return String.valueOf(Character.toChars(emoji)) + ' ' + InstantUtil.formatNodeName(ownerName);
    }

}
