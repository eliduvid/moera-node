package org.moera.node.rest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.Result;
import org.moera.node.model.SubscriberDescription;
import org.moera.node.model.SubscriberInfo;
import org.moera.node.model.ValidationFailure;
import org.moera.node.operations.InstantOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/people/subscribers")
public class SubscriberController {

    private static Logger log = LoggerFactory.getLogger(SubscriberController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private InstantOperations instantOperations;

    @GetMapping
    public List<SubscriberInfo> getAll(@RequestParam String nodeName,
                                       @RequestParam(defaultValue = "feed") SubscriptionType type)
            throws AuthenticationException {

        log.info("GET /people/subscribers (nodeName = {}, type = {})",
                LogUtil.format(nodeName), LogUtil.format(SubscriptionType.toValue(type)));

        String ownerName = requestContext.getClientName();
        if (!requestContext.isAdmin() && (StringUtils.isEmpty(ownerName) || !ownerName.equals(nodeName))) {
            throw new AuthenticationException();
        }

        return subscriberRepository.findByType(requestContext.nodeId(), nodeName, type).stream()
                .map(SubscriberInfo::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public SubscriberInfo get(@PathVariable UUID id) throws AuthenticationException {
        log.info("GET /people/subscribers/{id} (id = {})", LogUtil.format(id));

        Subscriber subscriber = subscriberRepository.findByNodeIdAndId(requestContext.nodeId(), id).orElse(null);
        if (subscriber == null) {
            throw new ObjectNotFoundFailure("subscriber.not-found");
        }
        String ownerName = requestContext.getClientName();
        if (!requestContext.isAdmin()
                && (StringUtils.isEmpty(ownerName) || !ownerName.equals(subscriber.getRemoteNodeName()))) {
            throw new AuthenticationException();
        }

        return new SubscriberInfo(subscriber);
    }

    @PostMapping
    @Transactional
    public SubscriberInfo post(@Valid @RequestBody SubscriberDescription subscriberDescription)
            throws AuthenticationException {

        log.info("POST /people/subscribers (type = {}, feedName = {}, postingId = {})",
                LogUtil.format(SubscriptionType.toValue(subscriberDescription.getType())),
                LogUtil.format(subscriberDescription.getFeedName()),
                LogUtil.format(subscriberDescription.getPostingId()));

        if (subscriberDescription.getType() == null) {
            throw new ValidationFailure("subscriberDescription.type.blank");
        }
        String ownerName = requestContext.getClientName();
        if (StringUtils.isEmpty(ownerName)) {
            throw new AuthenticationException();
        }
        if (similarExists(subscriberDescription)) {
            throw new OperationFailure("subscriber.already-exists");
        }
        validate(subscriberDescription);

        Subscriber subscriber = new Subscriber();
        subscriber.setId(UUID.randomUUID());
        subscriber.setNodeId(requestContext.nodeId());
        subscriber.setSubscriptionType(subscriberDescription.getType());
        subscriber.setRemoteNodeName(ownerName);
        if (!StringUtils.isEmpty(subscriberDescription.getFeedName())) {
            if (!Feed.isStandard(subscriberDescription.getFeedName())) {
                throw new ValidationFailure("subscriberDescription.feedName.not-found");
            }
            if (Feed.isAdmin(subscriberDescription.getFeedName())) {
                throw new ValidationFailure("subscriberDescription.feedName.not-found");
            }
            subscriber.setFeedName(subscriberDescription.getFeedName());
        }
        if (subscriberDescription.getPostingId() != null) {
            Posting posting = postingRepository.findByNodeIdAndId(
                    requestContext.nodeId(), subscriberDescription.getPostingId())
                    .orElse(null);
            if (posting == null) {
                throw new ValidationFailure("subscriberDescription.postingId.not-found");
            }
            subscriber.setEntry(posting);
        }
        subscriber = subscriberRepository.save(subscriber);
        instantOperations.subscriberAdded(subscriber);

        // TODO event
        return new SubscriberInfo(subscriber);
    }

    private boolean similarExists(SubscriberDescription description) {
        switch (description.getType()) {
            case FEED:
                return subscriberRepository.countByFeedName(requestContext.nodeId(), requestContext.getClientName(),
                        SubscriptionType.FEED, description.getFeedName()) > 0;
            case POSTING:
                return subscriberRepository.countByEntryId(requestContext.nodeId(), requestContext.getClientName(),
                        SubscriptionType.POSTING, description.getPostingId()) > 0;
        }
        return false; // Should never be reached
    }

    private void validate(SubscriberDescription description) {
        switch (description.getType()) {
            case FEED:
                if (StringUtils.isEmpty(description.getFeedName())) {
                    throw new ValidationFailure("subscriberDescription.feedName.blank");
                }
                break;
            case POSTING:
                if (description.getPostingId() == null) {
                    throw new ValidationFailure("subscriberDescription.postingId.blank");
                }
                break;
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result delete(@PathVariable UUID id) throws AuthenticationException {
        log.info("DELETE /people/subscribers/{id} (id = {})", LogUtil.format(id));

        Subscriber subscriber = subscriberRepository.findByNodeIdAndId(requestContext.nodeId(), id).orElse(null);
        if (subscriber == null) {
            throw new ObjectNotFoundFailure("subscriber.not-found");
        }
        String ownerName = requestContext.getClientName();
        if (!requestContext.isAdmin()
                && (StringUtils.isEmpty(ownerName) || !ownerName.equals(subscriber.getRemoteNodeName()))) {
            throw new AuthenticationException();
        }

        subscriberRepository.delete(subscriber);
        instantOperations.subscriberDeleted(subscriber);
        // TODO event

        return Result.OK;
    }

}
