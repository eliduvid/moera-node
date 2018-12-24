package org.moera.node.controller;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.inject.Inject;

import org.moera.node.model.RegisteredName;
import org.moera.node.naming.NamingClient;
import org.moera.node.option.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RegisteredNameController {

    private static Logger log = LoggerFactory.getLogger(RegisteredNameController.class);

    @Inject
    private Options options;

    @Inject
    private NamingClient namingClient;

    @PostMapping("/moera-node/registered-name")
    public void post(@RequestBody RegisteredName registeredName) throws NoSuchAlgorithmException { // TODO handle it
        log.info("Asked to register the name '{}'", registeredName.getName());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        SecureRandom random = SecureRandom.getInstanceStrong();
        keyPairGenerator.initialize(256, random);
        KeyPair updatingKeyPair = keyPairGenerator.generateKeyPair();
        keyPairGenerator.initialize(256, random);
        KeyPair signingKeyPair = keyPairGenerator.generateKeyPair();
        namingClient.register(registeredName.getName(), updatingKeyPair.getPublic(), signingKeyPair.getPublic());
        options.set("profile.registered-name", registeredName.getName());
        options.set("profile.signing-key", signingKeyPair.getPrivate());
    }

}
