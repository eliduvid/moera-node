package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "remote_comment_verifications")
public class RemoteCommentVerification {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @Size(max = 63)
    private String nodeName;

    @NotNull
    @Size(max = 40)
    private String postingId;

    @NotNull
    @Size(max = 40)
    private String commentId;

    @Size(max = 40)
    private String revisionId;

    @NotNull
    @Enumerated
    private VerificationStatus status = VerificationStatus.RUNNING;

    @Size(max = 63)
    private String errorCode;

    @Size(max = 255)
    private String errorMessage;

    @NotNull
    private Timestamp deadline;

    public RemoteCommentVerification() {
    }

    public RemoteCommentVerification(UUID nodeId, String nodeName, String postingId, String commentId,
                                     String revisionId) {
        this.id = UUID.randomUUID();
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.postingId = postingId;
        this.commentId = commentId;
        this.revisionId = revisionId;
    }

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

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(String revisionId) {
        this.revisionId = revisionId;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
    }

}
