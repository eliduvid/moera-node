package org.moera.node.rest.notification;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.instant.CommentReactionInstants;
import org.moera.node.model.notification.CommentReactionAddedNotification;
import org.moera.node.model.notification.CommentReactionDeletedAllNotification;
import org.moera.node.model.notification.CommentReactionDeletedNotification;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;

@NotificationProcessor
public class CommentReactionProcessor {

    @Inject
    private CommentReactionInstants commentReactionInstants;

    @NotificationMapping(NotificationType.COMMENT_REACTION_ADDED)
    @Transactional
    public void added(CommentReactionAddedNotification notification) {
        commentReactionInstants.added(notification.getSenderNodeName(), notification.getPostingId(),
                notification.getCommentId(), notification.getOwnerName(), notification.getCommentHeading(),
                notification.isNegative(), notification.getEmoji());
    }

    @NotificationMapping(NotificationType.COMMENT_REACTION_DELETED)
    @Transactional
    public void deleted(CommentReactionDeletedNotification notification) {
        commentReactionInstants.deleted(notification.getSenderNodeName(), notification.getPostingId(),
                notification.getCommentId(), notification.getOwnerName(), notification.isNegative());
    }

    @NotificationMapping(NotificationType.COMMENT_REACTION_DELETED_ALL)
    @Transactional
    public void deletedAll(CommentReactionDeletedAllNotification notification) {
        commentReactionInstants.deletedAll(notification.getSenderNodeName(), notification.getPostingId(),
                notification.getCommentId());
    }

}
