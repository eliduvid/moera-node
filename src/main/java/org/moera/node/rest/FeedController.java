package org.moera.node.rest;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.ReactionRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ClientReactionInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.StoryInfo;
import org.moera.node.model.StoryPostingAddedInfo;
import org.moera.node.model.FeedInfo;
import org.moera.node.model.FeedSliceInfo;
import org.moera.node.model.ValidationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/feeds")
public class FeedController {

    private static Logger log = LoggerFactory.getLogger(FeedController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private ReactionRepository reactionRepository;

    @GetMapping
    public List<FeedInfo> getAll() {
        log.info("GET /feeds");

        return Collections.singletonList(new FeedInfo(Feed.TIMELINE));
    }

    @GetMapping("/{feedName}")
    public FeedInfo get(@PathVariable String feedName) {
        log.info("GET /feeds/{feedName} (feedName = {})", LogUtil.format(feedName));

        if (!feedName.equals(Feed.TIMELINE)) {
            throw new ObjectNotFoundFailure("feed.not-found");
        }
        return new FeedInfo(feedName);
    }

    @GetMapping("/{feedName}/stories")
    public FeedSliceInfo getStories(
            @PathVariable String feedName,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit) {

        log.info("GET /feeds/{feedName}/stories (feedName = {}, before = {}, after = {}, limit = {})",
                LogUtil.format(feedName), LogUtil.format(before), LogUtil.format(after), LogUtil.format(limit));

        if (!feedName.equals(Feed.TIMELINE)) {
            throw new ObjectNotFoundFailure("feed.not-found");
        }
        if (before != null && after != null) {
            throw new ValidationFailure("feed.before-after-exclusive");
        }

        limit = limit != null && limit <= PostingOperations.MAX_POSTINGS_PER_REQUEST
                ? limit : PostingOperations.MAX_POSTINGS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }
        if (after == null) {
            before = before != null ? before : Long.MAX_VALUE;
            return getStoriesBefore(feedName, before, limit);
        } else {
            return getStoriesAfter(feedName, after, limit);
        }
    }

    private FeedSliceInfo getStoriesBefore(String feedName, long before, int limit) {
        Page<Story> page = storyRepository.findSlice(requestContext.nodeId(), feedName, Long.MIN_VALUE, before,
                PageRequest.of(0, limit + 1, Sort.Direction.DESC, "moment"));
        FeedSliceInfo sliceInfo = new FeedSliceInfo();
        sliceInfo.setBefore(before);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setAfter(Long.MIN_VALUE);
        } else {
            sliceInfo.setAfter(page.getContent().get(limit).getMoment());
        }
        fillSlice(sliceInfo, feedName, limit);
        return sliceInfo;
    }

    private FeedSliceInfo getStoriesAfter(String feedName, long after, int limit) {
        Page<Story> page = storyRepository.findSlice(requestContext.nodeId(), feedName, after, Long.MAX_VALUE,
                PageRequest.of(0, limit + 1, Sort.Direction.ASC, "moment"));
        FeedSliceInfo sliceInfo = new FeedSliceInfo();
        sliceInfo.setAfter(after);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setBefore(Long.MAX_VALUE);
        } else {
            sliceInfo.setBefore(page.getContent().get(limit - 1).getMoment());
        }
        fillSlice(sliceInfo, feedName, limit);
        return sliceInfo;
    }

    private void fillSlice(FeedSliceInfo sliceInfo, String feedName, int limit) {
        List<StoryInfo> stories = storyRepository.findInRange(
                requestContext.nodeId(), feedName, sliceInfo.getAfter(), sliceInfo.getBefore())
                .stream()
                .map(this::buildStoryInfo)
                .sorted(Comparator.comparing(StoryInfo::getMoment).reversed())
                .collect(Collectors.toList());
        String clientName = requestContext.getClientName();
        if (!StringUtils.isEmpty(clientName)) {
            Map<String, PostingInfo> postingMap = stories.stream()
                    .map(s -> ((StoryPostingAddedInfo) s).getPosting())
                    .collect(Collectors.toMap(PostingInfo::getId, Function.identity()));
            reactionRepository.findByStoriesInRangeAndOwner(
                    requestContext.nodeId(), feedName, sliceInfo.getAfter(), sliceInfo.getBefore(), clientName)
                    .stream()
                    .map(ClientReactionInfo::new)
                    .filter(r -> postingMap.containsKey(r.getPostingId()))
                    .forEach(r -> postingMap.get(r.getPostingId()).setClientReaction(r));
        }
        if (stories.size() > limit) {
            stories.remove(limit);
        }
        sliceInfo.setStories(stories);
    }

    private StoryPostingAddedInfo buildStoryInfo(Story story) {
        Posting posting = (Posting) story.getEntry();
        return new StoryPostingAddedInfo(
                story,
                new PostingInfo(posting, requestContext.isAdmin() || requestContext.isClient(posting.getOwnerName())),
                requestContext.isAdmin()
        );
    }

}
