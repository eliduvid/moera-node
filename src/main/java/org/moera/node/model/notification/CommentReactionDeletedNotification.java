package org.moera.node.model.notification;

import java.util.UUID;

public class CommentReactionDeletedNotification extends CommentReactionNotification {

    public CommentReactionDeletedNotification() {
        super(NotificationType.COMMENT_REACTION_DELETED);
    }

    public CommentReactionDeletedNotification(UUID postingId, UUID commentId, String ownerName, boolean negative) {
        super(NotificationType.COMMENT_REACTION_DELETED, postingId, commentId, ownerName, negative);
    }

}
