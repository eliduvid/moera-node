package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Story;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeedReference {

    private String feedName;
    private Long publishedAt;
    private boolean pinned;
    private Long moment;
    private String storyId;
    private Map<String, String[]> operations;

    public FeedReference() {
    }

    public FeedReference(Story story) {
        feedName = story.getFeedName();
        publishedAt = Util.toEpochSecond(story.getPublishedAt());
        pinned = story.isPinned();
        moment = story.getMoment();
        storyId = story.getId().toString();
        operations = new HashMap<>();
        operations.put("edit", new String[]{"admin"});
        operations.put("delete", new String[]{"admin"});
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public Long getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Long publishedAt) {
        this.publishedAt = publishedAt;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public Long getMoment() {
        return moment;
    }

    public void setMoment(Long moment) {
        this.moment = moment;
    }

    public String getStoryId() {
        return storyId;
    }

    public void setStoryId(String storyId) {
        this.storyId = storyId;
    }

    public Map<String, String[]> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String[]> operations) {
        this.operations = operations;
    }

}
