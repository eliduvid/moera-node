package org.moera.node.model.event;

import org.moera.node.data.Posting;

public class PostingEvent extends Event {

    private String id;

    protected PostingEvent(EventType type) {
        super(type);
    }

    protected PostingEvent(EventType type, Posting posting) {
        super(type);
        this.id = posting.getId().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
