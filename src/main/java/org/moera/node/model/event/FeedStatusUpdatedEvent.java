package org.moera.node.model.event;

import org.moera.node.event.EventSubscriber;
import org.moera.node.model.FeedStatus;

public class FeedStatusUpdatedEvent extends Event {

    private String feedName;
    private int notViewed;
    private int notRead;

    public FeedStatusUpdatedEvent() {
        super(EventType.FEED_STATUS_UPDATED);
    }

    public FeedStatusUpdatedEvent(String feedName, FeedStatus feedStatus) {
        super(EventType.FEED_STATUS_UPDATED);

        this.feedName = feedName;
        notViewed = feedStatus.getNotViewed();
        notRead = feedStatus.getNotRead();
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public int getNotViewed() {
        return notViewed;
    }

    public void setNotViewed(int notViewed) {
        this.notViewed = notViewed;
    }

    public int getNotRead() {
        return notRead;
    }

    public void setNotRead(int notRead) {
        this.notRead = notRead;
    }

    @Override
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isAdmin();
    }

}
