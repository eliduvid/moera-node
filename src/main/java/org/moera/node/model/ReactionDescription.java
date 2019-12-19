package org.moera.node.model;

import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionTotal;

public class ReactionDescription {

    private boolean negative;
    private int emoji;

    public boolean isNegative() {
        return negative;
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

    public int getEmoji() {
        return emoji;
    }

    public void setEmoji(int emoji) {
        this.emoji = emoji;
    }

    public void toReaction(Reaction reaction) {
        reaction.setNegative(negative);
        reaction.setEmoji(emoji);
    }

    public void toReactionTotal(ReactionTotal total) {
        total.setNegative(negative);
        total.setEmoji(emoji);
    }

}