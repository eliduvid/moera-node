package org.moera.node.naming;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import org.moera.commons.util.CryptoUtil;
import org.moera.commons.util.Util;
import org.moera.naming.rpc.NamingService;
import org.moera.naming.rpc.OperationStatusInfo;
import org.moera.node.option.Options;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Service
public class NamingClient {

    private NamingService namingService;

    @Inject
    private Options options;

    @Inject
    private TaskScheduler taskScheduler;

    @PostConstruct
    protected void init() throws MalformedURLException {
        JsonRpcHttpClient client = new JsonRpcHttpClient(new URL(options.getString("naming.location")));
        namingService = ProxyUtil.createClientProxy(getClass().getClassLoader(), NamingService.class, client);
        monitorOperation();
    }

    private void monitorOperation() {
        if (options.getUuid("naming.operation.id") == null) {
            return;
        }
        taskScheduler.schedule(() -> {
            UUID id = options.getUuid("naming.operation.id");
            if (id == null) {
                return;
            }
            OperationStatusInfo info;
            try {
                info = namingService.getStatus(id);
            } catch (Exception e) {
                // TODO count retries and mark as unknown
                return;
            }
            if (info.getStatus() == null) {
                options.set("naming.operation.status", "unknown");
                options.set("naming.operation.error-code", "naming." + info.getErrorCode());
                options.reset("naming.operation.id");
                return;
            }
            options.set("naming.operation.status", info.getStatus().name().toLowerCase());
            options.set("naming.operation.added", info.getAdded());
            switch (info.getStatus()) {
                case ADDED:
                case STARTED:
                    break;
                case SUCCEEDED:
                    options.set("naming.operation.completed", info.getCompleted());
                    options.set("profile.registered-name.generation", info.getGeneration());
                    options.reset("naming.operation.id");
                    break;
                case FAILED:
                    options.set("naming.operation.completed", info.getCompleted());
                    options.set("naming.operation.error-code", "naming." + info.getErrorCode());
                    options.reset("naming.operation.id");
                    break;
            }
        }, context -> {
            if (options.getUuid("naming.operation.id") == null) {
                return null;
            }
            Date last = context.lastCompletionTime();
            return last == null ? new Date() : Date.from(last.toInstant().plusSeconds(60));
        });
    }

    public void register(String name, PublicKey updatingKey, PublicKey signingKey) {
        String updatingKeyE = Util.base64encode(CryptoUtil.toRawPublicKey(updatingKey));
        String signingKeyE = Util.base64encode(CryptoUtil.toRawPublicKey(signingKey));
        long validFrom = Instant.now()
                                .plus(options.getDuration("profile.signing-key.valid-from.layover"))
                                .getEpochSecond();
        UUID operationId;
        try {
            operationId = namingService.put(name, false, updatingKeyE, "", signingKeyE, validFrom, null);
        } catch (Exception e) {
            throw new NamingNotAvailableException(e);
        }
        options.set("naming.operation.id", operationId);
        options.set("profile.registered-name", name);
        monitorOperation();
    }

}
