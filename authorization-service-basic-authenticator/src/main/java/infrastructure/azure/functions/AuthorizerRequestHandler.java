package infrastructure.azure.functions;

import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import core.HttpBasicAuthenticator;
import core.Principal;
import infrastructure.azure.AzureFactory;
import com.microsoft.azure.functions.*;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shared.infrastructure.azure.gateway.AuthorizerRequest;
import shared.infrastructure.azure.gateway.AuthorizerResponse;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

public class AuthorizerRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizerRequestHandler.class);
    private final AzureFactory factory;
    private static final String REQUIRED_ROLE_ID = "your-required-role-id"; // Define the required role ID here

    public AuthorizerRequestHandler() {
        this.factory = AzureFactory.getInstance();
    }

    @FunctionName("AuthorizerRequestHandler")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION)
            HttpRequestMessage<Optional<AuthorizerRequest>> request,
            final ExecutionContext context) {

        HttpBasicAuthenticator authenticator = factory.createHttpBasicAuthenticator();
        Principal principal = new Principal();
        AuthorizerResponse response;

        try {
            String authToken = request.getHeaders().get("Authorization");
            if (authToken == null || authToken.isEmpty()) {
                return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
                        .body("Authorization token is missing").build();
            }

            // Authenticate with Azure AD
            principal = authenticator.authenticate(authToken);
            logger.info("User {} authenticated successfully.", principal.getUsername());

            // Check if user has required role
            if (!hasRequiredRole(principal, authToken)) {
                logger.warn("User {} does not have the required role.", principal.getUsername());
                return request.createResponseBuilder(HttpStatus.FORBIDDEN)
                        .body("User does not have the required role").build();
            }

            // Build the authorizer response without a custom Policy document
            response = new AuthorizerResponse.Builder()
                    .withPrincipalId(principal.getId())
                    .withContextAsString("username", principal.getUsername())
                    .withContextAsString("scope", Optional.ofNullable(principal.getScope())
                            .map(o -> o.stream().collect(Collectors.joining(","))).orElse(null))
                    .withContextAsNumber("expirationTime", principal.getExpirationTime())
                    .build();

            return request.createResponseBuilder(HttpStatus.OK)
                    .body(response)
                    .build();

        } catch (Exception e) {
            logger.error("Authentication failed: {}", e.getMessage(), e);
            return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
                    .body("Unauthorized: " + e.getMessage())
                    .build();
        }
    }

    private boolean hasRequiredRole(Principal principal, String authToken) {
        // Configure Graph client
        TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(Collections.singletonList("https://graph.microsoft.com/.default"), factory.getAzureIdentityClient());
        GraphServiceClient<?> graphClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();

        // Fetch user's roles
        var roles = graphClient.users(principal.getId()).appRoleAssignments().buildRequest().get();

        // Check if the required role ID is among the user's roles
        return roles.getCurrentPage().stream()
                .anyMatch(role -> REQUIRED_ROLE_ID.equals(role.appRoleId));
    }
}
