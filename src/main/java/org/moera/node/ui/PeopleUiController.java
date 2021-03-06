package org.moera.node.ui;

import java.util.List;
import javax.inject.Inject;

import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UiController;
import org.moera.node.global.VirtualPage;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@UiController
public class PeopleUiController {

    @Inject
    private RequestContext requestContext;

    @Inject
    private TitleBuilder titleBuilder;

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @GetMapping("/people")
    public String people() {
        return "redirect:/people/subscribers";
    }

    @GetMapping("/people/subscribers")
    @VirtualPage
    public String subscribers(Model model) {
        int subscribersTotal = subscriberRepository.countByType(requestContext.nodeId(), SubscriptionType.FEED);
        int subscriptionsTotal = subscriptionRepository.countByType(requestContext.nodeId(), SubscriptionType.FEED);
        List<Subscriber> subscribers = subscriberRepository.findAllByType(requestContext.nodeId(),
                SubscriptionType.FEED);

        model.addAttribute("pageTitle", titleBuilder.build("Subscribers"));
        model.addAttribute("menuIndex", "people");
        model.addAttribute("subscribersTotal", subscribersTotal);
        model.addAttribute("subscriptionsTotal", subscriptionsTotal);
        model.addAttribute("subscribers", subscribers);

        return "subscribers";
    }

    @GetMapping("/people/subscriptions")
    @VirtualPage
    public String subscriptions(Model model) {
        int subscribersTotal = subscriberRepository.countByType(requestContext.nodeId(), SubscriptionType.FEED);
        int subscriptionsTotal = subscriptionRepository.countByType(requestContext.nodeId(), SubscriptionType.FEED);
        List<Subscription> subscriptions = subscriptionRepository.findAllByType(requestContext.nodeId(),
                SubscriptionType.FEED);

        model.addAttribute("pageTitle", titleBuilder.build("Subscriptions"));
        model.addAttribute("menuIndex", "people");
        model.addAttribute("subscribersTotal", subscribersTotal);
        model.addAttribute("subscriptionsTotal", subscriptionsTotal);
        model.addAttribute("subscriptions", subscriptions);

        return "subscriptions";
    }

}
