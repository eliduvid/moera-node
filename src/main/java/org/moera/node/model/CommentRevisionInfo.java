package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.SourceFormat;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentRevisionInfo implements RevisionInfo {

    private String id;
    private Body bodyPreview;
    private byte[] bodySrcHash;
    private SourceFormat bodySrcFormat;
    private Body body;
    private String bodyFormat;
    private String heading;
    private Long createdAt;
    private Long deletedAt;
    private Long deadline;
    private byte[] digest;
    private byte[] signature;
    private Short signatureVersion;
    private ClientReactionInfo clientReaction;
    private ReactionTotalsInfo reactions;

    public CommentRevisionInfo() {
    }

    public CommentRevisionInfo(EntryRevision revision, boolean countsVisible) {
        id = revision.getId().toString();
        bodyPreview = new Body(revision.getBodyPreview());
        bodySrcHash = revision.getReceiverBodySrcHash() != null
                ? revision.getReceiverBodySrcHash()
                : CryptoUtil.digest(revision.getBodySrc());
        bodySrcFormat = revision.getBodySrcFormat();
        body = new Body(revision.getBody());
        bodyFormat = revision.getBodyFormat();
        heading = revision.getHeading();
        createdAt = Util.toEpochSecond(revision.getCreatedAt());
        deletedAt = Util.toEpochSecond(revision.getDeletedAt());
        deadline = Util.toEpochSecond(revision.getDeadline());
        digest = revision.getDigest();
        signature = revision.getSignature();
        signatureVersion = revision.getSignatureVersion();
        reactions = new ReactionTotalsInfo(revision.getReactionTotals(), countsVisible);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Body getBodyPreview() {
        return bodyPreview;
    }

    public void setBodyPreview(Body bodyPreview) {
        this.bodyPreview = bodyPreview;
    }

    public byte[] getBodySrcHash() {
        return bodySrcHash;
    }

    public void setBodySrcHash(byte[] bodySrcHash) {
        this.bodySrcHash = bodySrcHash;
    }

    public SourceFormat getBodySrcFormat() {
        return bodySrcFormat;
    }

    public void setBodySrcFormat(SourceFormat bodySrcFormat) {
        this.bodySrcFormat = bodySrcFormat;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public String getBodyFormat() {
        return bodyFormat;
    }

    public void setBodyFormat(String bodyFormat) {
        this.bodyFormat = bodyFormat;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    @Override
    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public Long getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Long deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public byte[] getDigest() {
        return digest;
    }

    public void setDigest(byte[] digest) {
        this.digest = digest;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public Short getSignatureVersion() {
        return signatureVersion;
    }

    public void setSignatureVersion(Short signatureVersion) {
        this.signatureVersion = signatureVersion;
    }

    public ClientReactionInfo getClientReaction() {
        return clientReaction;
    }

    public void setClientReaction(ClientReactionInfo clientReaction) {
        this.clientReaction = clientReaction;
    }

    public ReactionTotalsInfo getReactions() {
        return reactions;
    }

    public void setReactions(ReactionTotalsInfo reactions) {
        this.reactions = reactions;
    }

}
