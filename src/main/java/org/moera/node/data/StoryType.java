package org.moera.node.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StoryType {

    POSTING_ADDED,
    REACTION_ADDED_POSITIVE,
    REACTION_ADDED_NEGATIVE,
    MENTION_POSTING,
    SUBSCRIBER_ADDED,
    SUBSCRIBER_DELETED,
    COMMENT_ADDED,
    MENTION_COMMENT,
    REPLY_COMMENT,
    COMMENT_REACTION_ADDED_POSITIVE,
    COMMENT_REACTION_ADDED_NEGATIVE,
    REMOTE_COMMENT_ADDED;

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static String toValue(StoryType type) {
        return type != null ? type.getValue() : null;
    }

    public static StoryType forValue(String value) {
        try {
            return parse(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonCreator
    public static StoryType parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_'));
    }

}
