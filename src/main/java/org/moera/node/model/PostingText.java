package org.moera.node.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.moera.node.data.Posting;
import org.moera.node.data.SourceFormat;
import org.moera.node.util.Util;
import org.springframework.util.StringUtils;

public class PostingText {

    @NotBlank
    @Size(max = 65535)
    private String bodySrc;

    private String bodySrcFormat;

    @NotBlank
    @Size(max = 65535)
    private String bodyHtml;

    private Long created;

    public PostingText() {
    }

    public String getBodySrc() {
        return bodySrc;
    }

    public void setBodySrc(String bodySrc) {
        this.bodySrc = bodySrc;
    }

    public String getBodySrcFormat() {
        return bodySrcFormat;
    }

    public void setBodySrcFormat(String bodySrcFormat) {
        this.bodySrcFormat = bodySrcFormat;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public void toPosting(Posting posting) {
        posting.setBodySrc(bodySrc);
        if (!StringUtils.isEmpty(bodySrcFormat)) {
            SourceFormat format = SourceFormat.forValue(bodySrcFormat);
            if (format == null) {
                throw new ValidationFailure("postingText.bodySrcFormat.unknown");
            }
            posting.setBodySrcFormat(format);
        }
        posting.setBodyHtml(bodyHtml);
        if (created != null) {
            posting.setCreated(Util.toTimestamp(created));
        }
    }

}