package org.moera.node.model.event;

import org.moera.node.data.Story;

public class StoryUpdatedEvent extends StoryEvent {

    public StoryUpdatedEvent() {
        super(EventType.STORY_UPDATED);
    }

    public StoryUpdatedEvent(Story story, boolean isAdmin) {
        super(EventType.STORY_UPDATED, story, isAdmin);
    }

}
