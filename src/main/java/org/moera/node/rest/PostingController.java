package org.moera.node.rest;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.util.LogUtil;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.PublicPage;
import org.moera.node.data.PublicPageRepository;
import org.moera.node.data.fingerprint.PostingFingerprint;
import org.moera.node.global.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingText;
import org.moera.node.option.Options;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@ApiController
@RequestMapping("/moera/api/postings")
public class PostingController {

    private static final int PUBLIC_PAGE_MAX_SIZE = 30;
    private static final int PUBLIC_PAGE_AVG_SIZE = 20;

    private static Logger log = LoggerFactory.getLogger(PostingController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private PublicPageRepository publicPageRepository;

    private AtomicInteger nonce = new AtomicInteger(0);

    @PostMapping
    @Admin
    @ResponseBody
    @Transactional
    public PostingInfo post(@Valid @RequestBody PostingText postingText) throws IOException, GeneralSecurityException {
        log.info("POST /postings (bodySrc = {}, bodySrcFormat = {}, bodyHtml = {})",
                LogUtil.format(postingText.getBodySrc(), 64),
                LogUtil.format(postingText.getBodySrcFormat()),
                LogUtil.format(postingText.getBodyHtml(), 64));

        Options options = requestContext.getOptions();
        String name = options.getString("profile.registered-name");
        Integer generation = options.getInt("profile.registered-name.generation");
        if (name == null || generation == null) {
            throw new OperationFailure("posting.registered-name-not-set");
        }
        PrivateKey signingKey = options.getPrivateKey("profile.signing-key");
        if (signingKey == null) {
            throw new OperationFailure("posting.signing-key-not-set");
        }

        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(options.nodeId());
        posting.setOwnerName(name);
        posting.setOwnerGeneration(generation);
        postingText.toPosting(posting);
        posting.setMoment(buildMoment(posting.getCreated()));
        posting.setSignature(CryptoUtil.sign(new PostingFingerprint(posting), (ECPrivateKey) signingKey));
        postingRepository.saveAndFlush(posting);
        updatePublicPages(posting.getMoment());

        return new PostingInfo(posting);
    }

    private long buildMoment(Timestamp timestamp) {
        return Util.toEpochSecond(timestamp) * 100 + nonce.getAndIncrement() % 100;
    }

    private void updatePublicPages(long moment) {
        PublicPage firstPage = publicPageRepository.findByEndMoment(Long.MAX_VALUE);
        if (firstPage == null) {
            firstPage = new PublicPage();
            firstPage.setNodeId(requestContext.nodeId());
            firstPage.setBeginMoment(0);
            firstPage.setEndMoment(Long.MAX_VALUE);
            publicPageRepository.save(firstPage);
            return;
        }

        long begin = firstPage.getBeginMoment();
        if (moment > begin) {
            int count = postingRepository.countInRange(begin, Long.MAX_VALUE);
            if (count >= PUBLIC_PAGE_MAX_SIZE) {
                long median = postingRepository.findMomentsInRange(begin, Long.MAX_VALUE,
                        PageRequest.of(count - PUBLIC_PAGE_AVG_SIZE, 1, Sort.by(Sort.Direction.DESC, "moment")))
                        .getContent().get(0);
                firstPage.setBeginMoment(median);
                PublicPage secondPage = new PublicPage();
                secondPage.setNodeId(requestContext.nodeId());
                secondPage.setBeginMoment(begin);
                secondPage.setEndMoment(median);
                publicPageRepository.save(secondPage);
            }
            return;
        }

        PublicPage lastPage = publicPageRepository.findByBeginMoment(0);
        long end = lastPage.getEndMoment();
        if (moment <= end) {
            int count = postingRepository.countInRange(0, end);
            if (count >= PUBLIC_PAGE_MAX_SIZE) {
                long median = postingRepository.findMomentsInRange(0, end,
                        PageRequest.of(PUBLIC_PAGE_AVG_SIZE + 1, 1, Sort.by(Sort.Direction.DESC, "moment")))
                        .getContent().get(0);
                lastPage.setEndMoment(median);
                PublicPage prevPage = new PublicPage();
                prevPage.setNodeId(requestContext.nodeId());
                prevPage.setBeginMoment(median);
                prevPage.setEndMoment(end);
                publicPageRepository.save(prevPage);
            }
        }
    }

}