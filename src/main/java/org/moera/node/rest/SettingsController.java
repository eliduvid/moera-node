package org.moera.node.rest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.node.auth.Admin;
import org.moera.node.model.event.ClientSettingsChangedEvent;
import org.moera.node.model.event.NodeSettingsChangedEvent;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.Result;
import org.moera.node.model.SettingInfo;
import org.moera.node.model.SettingMetaInfo;
import org.moera.node.option.OptionDescriptor;
import org.moera.node.option.OptionsMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/settings")
public class SettingsController {

    private static Logger log = LoggerFactory.getLogger(SettingsController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private OptionsMetadata optionsMetadata;

    private List<SettingInfo> getOptions(Predicate<String> nameFilter) {
        List<SettingInfo> list = new ArrayList<>();
        requestContext.getOptions().forEach((name, value, optionType) -> {
            if (!nameFilter.test(name)) {
                return;
            }
            list.add(new SettingInfo(name, optionType.getString(value)));
        });
        list.sort(Comparator.comparing(SettingInfo::getName));

        return list;
    }

    @GetMapping("/node")
    @Admin
    public List<SettingInfo> getForNode(@RequestParam(required = false) String prefix) {
        log.info("GET /settings/node");

        return getOptions(name -> !name.startsWith(OptionsMetadata.CLIENT_PREFIX)
                && (prefix == null || name.startsWith(prefix)));
    }

    @GetMapping("/client")
    @Admin
    public List<SettingInfo> getForClient(@RequestParam(required = false) String prefix) {
        log.info("GET /settings/client");

        return getOptions(name -> name.startsWith(OptionsMetadata.CLIENT_PREFIX)
                && (prefix == null || name.startsWith(prefix)));
    }

    @GetMapping("/node/metadata")
    @Admin
    public List<SettingMetaInfo> getMetadata(@RequestParam(required = false) String prefix) {
        log.info("GET /settings/node/metadata");

        return optionsMetadata.getDescriptors().values().stream()
                .filter(d -> !d.isInternal())
                .filter(d -> prefix == null || d.getName().startsWith(prefix))
                .map(SettingMetaInfo::new)
                .sorted(Comparator.comparing(SettingMetaInfo::getName))
                .collect(Collectors.toList());
    }

    @PutMapping
    @Transactional
    @Admin
    public Result put(@RequestBody @Valid List<SettingInfo> settings) {
        log.info("PUT /settings");

        AtomicBoolean nodeChanged = new AtomicBoolean(false);
        AtomicBoolean clientChanged = new AtomicBoolean(false);
        requestContext.getOptions().runInTransaction(options -> {
            settings.forEach(setting -> {
                OptionDescriptor descriptor = optionsMetadata.getDescriptor(setting.getName());
                if (descriptor == null) {
                    throw new OperationFailure("setting.unknown");
                }
                if (descriptor.isInternal()) {
                    throw new OperationFailure("setting.internal");
                }
                if (setting.getValue() != null) {
                    options.set(setting.getName(), setting.getValue());
                } else {
                    options.reset(setting.getName());
                }
                if (setting.getName().startsWith(OptionsMetadata.CLIENT_PREFIX)) {
                    clientChanged.set(true);
                } else {
                    nodeChanged.set(true);
                }
            });
        });

        if (nodeChanged.get()) {
            requestContext.send(new NodeSettingsChangedEvent());
        }
        if (clientChanged.get()) {
            requestContext.send(new ClientSettingsChangedEvent());
        }

        return Result.OK;
    }

}
