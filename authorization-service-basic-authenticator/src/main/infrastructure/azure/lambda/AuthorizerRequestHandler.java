package com.github.vitalibo.authorization.basic.infrastructure.azure.functions;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.github.vitalibo.authorization.basic.core.HttpBasicAuthenticator;
import com.github.vitalibo.authorization.basic.core.Principal;
import com.github.vitalibo.authorization.basic.infrastructure.azure.Factory;
import com.github.vitalibo.authorization.shared.core.http.BasicAuthenticationException;
import com.github.vitalibo.authorization.shared.infrastructure.azure.gateway.AuthorizerRequest;
import com.github.vitalibo.authorization.shared.infrastructure.azure.gateway.AuthorizerResponse;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
public class AuthorizerRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizerRequestHandler.class);

    private final Factory factory;

    public AuthorizerRequestHandler() {
        this(Factory.getInstance());
    }

    @FunctionName("AuthorizerRequestHandler")
    public HttpResponseMessage handleRequest(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) AuthorizerRequest request,
            final ExecutionContext context) {
        Statement.Effect effect = Statement.Effect.Deny;
        HttpBasicAuthenticator authenticator = factory.createHttpBasicAuthenticator();

        Principal principal = new Principal();
        try {
            principal = authenticator.authenticate(request.getAuthorizationToken());

            effect = Statement.Effect.Allow;
        } catch (BasicAuthenticationException e) {
            logger.warn("Validation error. {}", e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }

        AuthorizerResponse response = new AuthorizerResponse.Builder()
            .withPrincipalId(principal.getId())
            .withPolicyDocument(new Policy()
                .withStatements(new Statement(effect)
                    .withActions(() -> "execute-api:Invoke")
                    .withResources(new Resource(request.getMethodArn()))))
            .withContextAsString("username", principal.getUsername())
            .withContextAsString("scope", Optional.ofNullable(principal.getScope())
                .map(o -> o.stream().collect(Collectors.joining(","))).orElse(null))
            .withContextAsNumber("expirationTime", principal.getExpirationTime())
            .build();

        return request.createResponseBuilder(HttpStatus.OK).body(response).build();
    }

}