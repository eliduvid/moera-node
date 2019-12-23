package org.moera.node.rest;

import java.lang.reflect.Constructor;
import java.util.Locale;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.naming.rpc.RegisteredNameInfo;
import org.moera.node.data.RemotePostingVerification;
import org.moera.node.data.RemotePostingVerificationRepository;
import org.moera.node.data.VerificationStatus;
import org.moera.node.domain.Domains;
import org.moera.node.event.EventManager;
import org.moera.node.event.model.RemotePostingVerificationFailedEvent;
import org.moera.node.event.model.RemotePostingVerifiedEvent;
import org.moera.node.fingerprint.FingerprintManager;
import org.moera.node.fingerprint.FingerprintObjectType;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.naming.NamingClient;
import org.moera.node.naming.NodeName;
import org.moera.node.naming.RegisteredName;
import org.moera.node.option.Options;
import org.moera.node.util.UriUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class RemotePostingVerifyTask implements Runnable {

    private static Logger log = LoggerFactory.getLogger(RemotePostingVerifyTask.class);

    private RemotePostingVerification data;

    private String nodeUri;
    private byte[] signingKey;

    @Inject
    private NamingClient namingClient;

    @Inject
    private MessageSource messageSource;

    @Inject
    private Domains domains;

    @Inject
    private EventManager eventManager;

    @Inject
    private RemotePostingVerificationRepository remotePostingVerificationRepository;

    @Inject
    private FingerprintManager fingerprintManager;

    public RemotePostingVerifyTask(RemotePostingVerification data) {
        this.data = data;
    }

    private void initLoggingDomain() {
        MDC.put("domain", domains.getDomainName(data.getNodeId()));
    }

    @Override
    public void run() {
        fetchNodeUri();
        if (nodeUri == null) {
            failed("remote-node.not-found", null);
            return;
        }
        WebClient.create(String.format("%s/api/postings/%s", nodeUri, data.getPostingId()))
                .get()
                .retrieve()
                .bodyToMono(PostingInfo.class)
                .subscribe(this::verify, this::error);
    }

    private void fetchNodeUri() {
        Options options = domains.getDomainOptions(data.getNodeId());
        RegisteredName registeredName = (RegisteredName) NodeName.parse(data.getNodeName());
        RegisteredNameInfo nameInfo =
                namingClient.getCurrent(registeredName.getName(), registeredName.getGeneration(), options);
        if (nameInfo != null) {
            nodeUri = UriUtil.normalize(nameInfo.getNodeUri());
        }
    }

    private void fetchSigningKey(String ownerName, long at) {
        Options options = domains.getDomainOptions(data.getNodeId());
        RegisteredName registeredName = (RegisteredName) NodeName.parse(ownerName);
        RegisteredNameInfo nameInfo =
                namingClient.getPast(registeredName.getName(), registeredName.getGeneration(), at, options);
        signingKey = nameInfo != null ? nameInfo.getSigningKey() : null;
    }

    private void verify(PostingInfo postingInfo) {
        try {
            data.setReceiverName(postingInfo.getReceiverName());
            remotePostingVerificationRepository.saveAndFlush(data);

            if (data.getRevisionId() == null) {
                verifySignature(postingInfo);
            } else {
                WebClient.create(String.format("%s/api/postings/%s/revisions/%s",
                                                nodeUri, data.getPostingId(), data.getRevisionId()))
                        .get()
                        .retrieve()
                        .bodyToMono(PostingRevisionInfo.class)
                        .subscribe(r -> verifySignature(postingInfo, r), this::error);
            }
        } catch (Exception e) {
            failed("remote-node.invalid-answer", null);
        }
    }

    private Constructor<? extends Fingerprint> getFingerprintConstructor(short version, Class<?>... parameterTypes) {
        return fingerprintManager.getConstructor(FingerprintObjectType.POSTING, version, parameterTypes);
    }

    private void verifySignature(PostingInfo postingInfo) {
        fetchSigningKey(postingInfo.getOwnerName(), postingInfo.getEditedAt());
        if (signingKey == null) {
            succeeded(false);
            return;
        }
        data.setRevisionId(postingInfo.getRevisionId());
        Constructor<? extends Fingerprint> constructor = getFingerprintConstructor(
                postingInfo.getSignatureVersion(), PostingInfo.class);
        succeeded(CryptoUtil.verify(postingInfo.getSignature(), signingKey, constructor, postingInfo));
    }

    private void verifySignature(PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo) {
        fetchSigningKey(postingInfo.getOwnerName(), postingRevisionInfo.getCreatedAt());
        if (signingKey == null) {
            succeeded(false);
            return;
        }
        Constructor<? extends Fingerprint> constructor = getFingerprintConstructor(
                postingInfo.getSignatureVersion(), PostingInfo.class, PostingRevisionInfo.class);
        succeeded(CryptoUtil.verify(
                postingRevisionInfo.getSignature(), signingKey, constructor, postingInfo, postingRevisionInfo));
    }

    private void error(Throwable e) {
        if (e instanceof WebClientResponseException) {
            if (((WebClientResponseException) e).getStatusCode() == HttpStatus.NOT_FOUND) {
                failed("remote-node.object-not-found", null);
            } else {
                failed("remote-node.internal-error", null);
            }
        } else {
            failed("remote-node.invalid-answer", e.getMessage());
        }
    }

    private void succeeded(boolean correct) {
        initLoggingDomain();
        log.info("Verified posting {}/{} at node {}: {}",
                data.getPostingId(), data.getRevisionId(), data.getNodeName(), correct ? "correct" : "incorrect");
        data.setStatus(correct ? VerificationStatus.CORRECT : VerificationStatus.INCORRECT);
        remotePostingVerificationRepository.saveAndFlush(data);
        eventManager.send(data.getNodeId(), new RemotePostingVerifiedEvent(data));
    }

    private void failed(String errorCode, String message) {
        initLoggingDomain();

        String errorMessage = messageSource.getMessage(errorCode, null, Locale.getDefault());
        if (message != null) {
            errorMessage += ": " + message;
        }
        log.info("Verification of posting {}/{} at node {} failed: {} ({})",
                data.getPostingId(), data.getRevisionId(), data.getNodeName(), errorMessage, errorCode);
        data.setStatus(VerificationStatus.ERROR);
        data.setErrorCode(errorCode);
        data.setErrorMessage(errorMessage);
        remotePostingVerificationRepository.saveAndFlush(data);
        eventManager.send(data.getNodeId(), new RemotePostingVerificationFailedEvent(data));
    }

}
