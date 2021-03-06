package org.moera.node.rest;

import java.sql.Timestamp;
import java.time.Instant;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.RemotePostingVerification;
import org.moera.node.data.RemotePostingVerificationRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.AsyncOperationCreated;
import org.moera.node.rest.task.RemotePostingVerifyTask;
import org.moera.node.task.TaskAutowire;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/nodes/{nodeName}/postings")
public class RemotePostingController {

    private static Logger log = LoggerFactory.getLogger(RemotePostingController.class);

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private RequestContext requestContext;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private RemotePostingVerificationRepository remotePostingVerificationRepository;

    @PostMapping("/{id}/verify")
    @Admin
    @Transactional
    public AsyncOperationCreated verify(@PathVariable String nodeName, @PathVariable String id) {
        log.info("POST /nodes/{name}/postings/{id}/verify, (name = {}, id = {})",
                LogUtil.format(nodeName), LogUtil.format(id));

        return executeVerifyTask(nodeName, id, null);
    }

    @PostMapping("/{id}/revisions/{revisionId}/verify")
    @Admin
    @Transactional
    public AsyncOperationCreated verifyRevision(@PathVariable String nodeName, @PathVariable String id,
                                 @PathVariable String revisionId) {
        log.info("POST /nodes/{name}/postings/{id}/revisions/{revisionId}/verify, (name = {}, id = {}, revisionId = {})",
                LogUtil.format(nodeName), LogUtil.format(id), LogUtil.format(revisionId));

        return executeVerifyTask(nodeName, id, revisionId);
    }

    private AsyncOperationCreated executeVerifyTask(String nodeName, String id, String revisionId) {
        RemotePostingVerification data = new RemotePostingVerification(
                requestContext.nodeId(), nodeName, id, revisionId);
        data.setDeadline(Timestamp.from(Instant.now().plus(
                requestContext.getOptions().getDuration("remote-posting-verification.lifetime"))));
        remotePostingVerificationRepository.saveAndFlush(data);

        RemotePostingVerifyTask task = new RemotePostingVerifyTask(data);
        taskAutowire.autowire(task);
        taskExecutor.execute(task);

        return new AsyncOperationCreated(data.getId());
    }

    @Scheduled(fixedDelayString = "PT30M")
    @Transactional
    public void purgeExpiredVerifications() {
        remotePostingVerificationRepository.deleteExpired(Util.now());
    }

}
