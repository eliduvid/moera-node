package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.commons.util.Util;

@Entity
@Table(name = "entries")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "entryType", discriminatorType = DiscriminatorType.INTEGER)
public class Entry {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    private UUID entryId;

    @NotNull
    @Enumerated
    @Column(insertable = false, updatable = false)
    private EntryType entryType;

    @NotNull
    @Size(max = 127)
    private String ownerName;

    @NotNull
    private int ownerGeneration;

    @NotNull
    private String bodyPreviewHtml = "";

    @NotNull
    private String bodySrc = "";

    @NotNull
    @Enumerated
    private SourceFormat bodySrcFormat = SourceFormat.PLAIN_TEXT;

    @NotNull
    private String bodyHtml = "";

    @NotNull
    private String heading = "";

    @NotNull
    private Timestamp createdAt = Util.now();

    @NotNull
    private Timestamp publishedAt = Util.now();

    private Timestamp deletedAt;

    @NotNull
    private long moment;

    @NotNull
    private byte[] signature;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public UUID getEntryId() {
        return entryId;
    }

    public void setEntryId(UUID entryId) {
        this.entryId = entryId;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public int getOwnerGeneration() {
        return ownerGeneration;
    }

    public void setOwnerGeneration(int ownerGeneration) {
        this.ownerGeneration = ownerGeneration;
    }

    public String getBodyPreviewHtml() {
        return bodyPreviewHtml;
    }

    public void setBodyPreviewHtml(String bodyPreviewHtml) {
        this.bodyPreviewHtml = bodyPreviewHtml;
    }

    public String getBodySrc() {
        return bodySrc;
    }

    public void setBodySrc(String bodySrc) {
        this.bodySrc = bodySrc;
    }

    public SourceFormat getBodySrcFormat() {
        return bodySrcFormat;
    }

    public void setBodySrcFormat(SourceFormat bodySrcFormat) {
        this.bodySrcFormat = bodySrcFormat;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Timestamp publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Timestamp getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Timestamp deletedAt) {
        this.deletedAt = deletedAt;
    }

    public long getMoment() {
        return moment;
    }

    public void setMoment(long moment) {
        this.moment = moment;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

}
