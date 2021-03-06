package org.moera.node.model.event;

import org.moera.node.data.Comment;

public class CommentEvent extends Event {

    private String id;
    private String postingId;
    private Long moment;

    protected CommentEvent(EventType type) {
        super(type);
    }

    protected CommentEvent(EventType type, Comment comment) {
        super(type);
        this.id = comment.getId().toString();
        this.postingId = comment.getPosting().getId().toString();
        this.moment = comment.getMoment();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public Long getMoment() {
        return moment;
    }

    public void setMoment(Long moment) {
        this.moment = moment;
    }

}
