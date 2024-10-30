package authorization.jwt.infrastructure.azure.functions;

import authorization.jwt.core.Claims;
import authorization.jwt.core.Jwt;
import authorization.jwt.core.JwtVerificationException;
import authorization.jwt.core.PolicyRepository;
import authorization.jwt.infrastructure.azure.Factory;
import com.azure.resourcemanager.authorization.models.RoleDefinition;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import shared.infrastructure.azure.gateway.AuthorizerRequest;
import shared.infrastructure.azure.gateway.AuthorizerResponse;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.FunctionName;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class AuthorizerRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizerRequestHandler.class);

    private final Factory factory;

    public AuthorizerRequestHandler() {
        this(Factory.getInstance());
    }

    @FunctionName("AuthorizerRequestHandler")
    public HttpResponseMessage handleRequest(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<AuthorizerRequest>> request,
            final ExecutionContext context) {

        Jwt jwt = factory.createJsonWebToken();
        PolicyRepository rolePolicyRepository = factory.createRolePolicyRepository();

        try {
            AuthorizerRequest authorizerRequest = request.getBody().orElseThrow(() ->
                    new JwtVerificationException("Authorization request is missing."));

            logger.warn("Jwt: {}", jwt);
            logger.warn("Auth token: {}", authorizerRequest.getAuthorizationToken());

            Claims claims = jwt.verify(authorizerRequest.getAuthorizationToken());

            List<RoleDefinition> roleDefinitions = rolePolicyRepository.getRoleDefinitions(claims);
            logger.warn("Claims: {}", claims);
            logger.warn("Role Definitions: {}", roleDefinitions);

            AuthorizerResponse authorizerResponse = new AuthorizerResponse.Builder()
                    .withPrincipalId(claims.getUsername())
                    .withRoleDefinition(roleDefinitions.get(0))
                    .build();

            return request.createResponseBuilder(HttpStatus.OK)
                    .body(authorizerResponse)
                    .build();

        } catch (JwtVerificationException e) {
            logger.warn("Unauthorized: {}", e.getMessage());
            return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
                    .body("Unauthorized")
                    .build();

        } catch (Exception e) {
            logger.error("Internal Server Error", e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal Server Error")
                    .build();
        }
    }
}
