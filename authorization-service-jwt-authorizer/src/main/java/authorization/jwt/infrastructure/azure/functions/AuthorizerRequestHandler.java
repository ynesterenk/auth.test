package authorization.jwt.infrastructure.azure.functions;

import authorization.jwt.core.Claims;
import authorization.jwt.core.Jwt;
import authorization.jwt.core.JwtVerificationException;
import authorization.jwt.core.PolicyRepository;
import authorization.jwt.infrastructure.azure.Factory;
import com.azure.resourcemanager.authorization.models.RoleDefinition;
import shared.infrastructure.azure.gateway.AuthorizerRequest;
import shared.infrastructure.azure.gateway.AuthorizerResponse;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RequiredArgsConstructor
public class AuthorizerRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizerRequestHandler.class);

    private final Factory factory;


    public AuthorizerRequestHandler() {
        this(Factory.getInstance());
    }

    @FunctionName("AuthorizerRequestHandler")
    public AuthorizerResponse handleRequest(AuthorizerRequest request, final ExecutionContext context) {
        
        Jwt jwt = factory.createJsonWebToken();
        PolicyRepository rolePolicyRepository = factory.createRolePolicyRepository();

        try {
            logger.warn("Jwt: {}", jwt);
            logger.warn("Auth token: {}", request.getAuthorizationToken());
            Claims claims = jwt.verify(request.getAuthorizationToken());

            List<RoleDefinition> roleDefinitions = rolePolicyRepository.getRoleDefinitions(claims);
            logger.warn("Claims: {}", claims);
            logger.warn("Role Definiations: {}", roleDefinitions);

            return new AuthorizerResponse.Builder()
                    .withPrincipalId(claims.getUsername())
                    .withRoleDefinition(roleDefinitions.get(0))
                    .build();

        } catch (JwtVerificationException e) {
            logger.warn("Unauthorized: {}", e.getMessage());
            //to do return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).body("Unauthorized").build();
            return  null;
        } catch (Exception e) {
            logger.error("Internal Server Error", e);
            throw new RuntimeException(e);
            //to do return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error").build();
        }
    }
}