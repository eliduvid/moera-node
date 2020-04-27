package org.moera.node.event.model;

import org.moera.node.data.Story;

public class StoryAddedEvent extends StoryEvent {

    public StoryAddedEvent() {
        super(EventType.STORY_ADDED);
    }

    public StoryAddedEvent(Story story, boolean isAdmin) {
        super(EventType.STORY_ADDED, story, isAdmin);
    }

}
