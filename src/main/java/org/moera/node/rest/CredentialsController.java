package org.moera.node.rest;

import java.net.URI;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.crypto.Password;
import org.moera.node.auth.Admin;
import org.moera.node.auth.RootAdmin;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.Credentials;
import org.moera.node.model.CredentialsCreated;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.Result;
import org.moera.node.option.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/credentials")
public class CredentialsController {

    private static Logger log = LoggerFactory.getLogger(CredentialsController.class);

    @Inject
    private RequestContext requestContext;

    @GetMapping
    public CredentialsCreated get() {
        log.info("GET /credentials");

        Options options = requestContext.getOptions();
        return new CredentialsCreated(
                !StringUtils.isEmpty(options.getString("credentials.login"))
                && !StringUtils.isEmpty(options.getString("credentials.password-hash")));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Result> post(@Valid @RequestBody Credentials credentials) {
        log.info("POST /credentials (login = '{}')", credentials.getLogin());

        requestContext.getOptions().runInTransaction(options -> {
            if (!StringUtils.isEmpty(options.getString("credentials.login"))
                    && !StringUtils.isEmpty(options.getString("credentials.password-hash"))) {
                throw new OperationFailure("credentials.already-created");
            }
            options.set("credentials.login", credentials.getLogin());
            options.set("credentials.password-hash", Password.hash(credentials.getPassword()));
        });

        return ResponseEntity.created(URI.create("/credentials")).body(Result.OK);
    }

    @PutMapping
    @Admin
    @Transactional
    public Result put(@Valid @RequestBody Credentials credentials) {
        log.info("PUT /credentials (login = '{}')", credentials.getLogin());

        requestContext.getOptions().runInTransaction(options -> {
            options.set("credentials.login", credentials.getLogin());
            options.set("credentials.password-hash", Password.hash(credentials.getPassword()));
        });

        return Result.OK;
    }

    @DeleteMapping
    @RootAdmin
    @Transactional
    public Result delete() {
        log.info("DELETE /credentials");

        requestContext.getOptions().runInTransaction(options -> {
            options.reset("credentials.login");
            options.reset("credentials.password-hash");
        });

        return Result.OK;
    }

}
