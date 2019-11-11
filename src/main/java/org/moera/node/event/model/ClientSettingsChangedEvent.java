package org.moera.node.event.model;

import org.moera.node.event.EventSubscriber;

public class ClientSettingsChangedEvent extends Event {

    public ClientSettingsChangedEvent() {
        super(EventType.CLIENT_SETTINGS_CHANGED);
    }

    @Override
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isAdmin();
    }

}
