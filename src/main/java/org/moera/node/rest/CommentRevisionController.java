package org.moera.node.rest;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.CommentRevisionInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.operations.ReactionTotalOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/postings/{postingId}/comments/{commentId}/revisions")
public class CommentRevisionController {

    private static Logger log = LoggerFactory.getLogger(CommentRevisionController.class);

    @Inject
    protected RequestContext requestContext;

    @Inject
    protected CommentRepository commentRepository;

    @Inject
    protected EntryRevisionRepository entryRevisionRepository;

    @Inject
    private ReactionTotalOperations reactionTotalOperations;

    @GetMapping
    public List<CommentRevisionInfo> getAll(@PathVariable UUID postingId, @PathVariable UUID commentId) {
        log.info("GET /postings/{postingId}/comments/{commentId}/revisions (postingId = {}, commentId = {})",
                LogUtil.format(postingId), LogUtil.format(commentId));

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
                .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }

        boolean countsVisible = reactionTotalOperations.isVisibleToClient(comment);
        return comment.getRevisions().stream()
                .map(r -> new CommentRevisionInfo(r, countsVisible))
                .sorted(Comparator.comparing(CommentRevisionInfo::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public PostingRevisionInfo get(@PathVariable UUID postingId, @PathVariable UUID commentId, @PathVariable UUID id) {
        log.info("GET /postings/{postingId}/comments/{commentId}/revisions/{id}"
                        + " (postingId = {}, commentId = {}, id = {})",
                LogUtil.format(postingId), LogUtil.format(commentId), LogUtil.format(id));

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
                .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }
        EntryRevision revision = entryRevisionRepository.findByEntryIdAndId(requestContext.nodeId(), commentId, id)
                .orElseThrow(() -> new ObjectNotFoundFailure("comment-revision.not-found"));

        return new PostingRevisionInfo(revision, reactionTotalOperations.isVisibleToClient(comment));
    }

}
