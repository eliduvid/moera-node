package org.moera.node.rest.notification;

import javax.inject.Inject;

import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.notification.NotificationType;
import org.moera.node.notification.model.MentionPostingAddedNotification;
import org.moera.node.notification.model.MentionPostingDeletedNotification;
import org.moera.node.rest.InstantOperations;

@NotificationProcessor
public class MentionPostingProcessor {

    @Inject
    private InstantOperations instantOperations;

    @NotificationMapping(NotificationType.MENTION_POSTING_ADDED)
    public void added(MentionPostingAddedNotification notification) {
        instantOperations.mentionPostingAdded(
                notification.getSenderNodeName(), notification.getPostingId(), notification.getHeading());
    }

    @NotificationMapping(NotificationType.MENTION_POSTING_DELETED)
    public void deleted(MentionPostingDeletedNotification notification) {
        instantOperations.mentionPostingDeleted(notification.getSenderNodeName(), notification.getPostingId());
    }

}