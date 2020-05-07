package org.moera.node.data;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.moera.commons.util.Util;

@Entity
@Table(name = "reactions")
public class Reaction {

    @Id
    private UUID id;

    @NotNull
    private String ownerName = "";

    @ManyToOne
    @NotNull
    private EntryRevision entryRevision;

    @NotNull
    private boolean negative;

    @NotNull
    private int emoji;

    @NotNull
    private long moment;

    @NotNull
    private Timestamp createdAt = Util.now();

    private Timestamp deadline;

    private Timestamp deletedAt;

    private byte[] signature;

    private short signatureVersion;

    @ManyToMany(mappedBy = "reactions")
    private Set<Story> stories = new HashSet<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public EntryRevision getEntryRevision() {
        return entryRevision;
    }

    public void setEntryRevision(EntryRevision entryRevision) {
        this.entryRevision = entryRevision;
    }

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

    public long getMoment() {
        return moment;
    }

    public void setMoment(long moment) {
        this.moment = moment;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
    }

    public Timestamp getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Timestamp deletedAt) {
        this.deletedAt = deletedAt;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public short getSignatureVersion() {
        return signatureVersion;
    }

    public void setSignatureVersion(short signatureVersion) {
        this.signatureVersion = signatureVersion;
    }

    public Set<Story> getStories() {
        return stories;
    }

    public void setStories(Set<Story> stories) {
        this.stories = stories;
    }

    public void addStory(Story story) {
        stories.add(story);
        story.getReactions().add(this);
    }

    public void removeStory(Story story) {
        stories.removeIf(t -> t.getId().equals(story.getId()));
        story.getReactions().removeIf(r -> r.getId().equals(id));
    }

    public void toReactionTotal(ReactionTotal total) {
        total.setNegative(negative);
        total.setEmoji(emoji);
    }

}
