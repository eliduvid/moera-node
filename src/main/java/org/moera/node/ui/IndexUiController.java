package org.moera.node.ui;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.global.PublicRequestContext;
import org.moera.node.global.UiController;
import org.moera.node.global.VirtualPage;
import org.moera.node.model.ProfileInfo;
import org.moera.node.model.RegisteredNameInfo;
import org.moera.node.option.Options;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@UiController
public class IndexUiController {

    @Inject
    private Options options;

    @GetMapping("/")
    @VirtualPage("/")
    private String index(Model model) {
        model.addAttribute("menuIndex", "index");

        return "index";
    }

    @GetMapping("/profile")
    @VirtualPage("/profile")
    public String profile(Model model, HttpServletResponse response) {
        model.addAttribute("menuIndex", "profile");
        model.addAttribute("registeredName", new RegisteredNameInfo(options, new PublicRequestContext()));
        model.addAttribute("profile", new ProfileInfo(options, new PublicRequestContext()));

        return "profile";
    }

}
