package authorization.server.infrastructure.azure.ad;

import authorization.server.core.UserPool;
import authorization.server.core.UserPoolException;
import com.microsoft.aad.msal4j.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class AzureADUserPool implements UserPool {

    private static final Logger logger = LoggerFactory.getLogger(AzureADUserPool.class);

    private final String clientId;
    private final String clientSecret;
    private final String authority;
    private PublicClientApplication publicClientApp;  // Injected application (optional)

    // New constructor with PublicClientApplication injection
    public AzureADUserPool(String clientId, String clientSecret, String authority, PublicClientApplication publicClientApp) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authority = authority;
        this.publicClientApp = publicClientApp;
    }

    @Override
    public String authenticate(String username, String password) throws UserPoolException {
        logger.info("Authenticating user {}", username);

        try {
            return acquireToken(username, password, Collections.singleton("User.Read"));
        } catch (Exception e) {
            throw new UserPoolException("Authentication failed for user " + username, e);
        }
    }

    @Override
    public void changePassword(String username, String previousPassword, String proposedPassword) throws UserPoolException {
        logger.warn("Azure AD does not support direct password change through MSAL.");
        throw new UnsupportedOperationException("Azure AD requires self-service password reset or admin intervention to change password.");
    }

    /**
     * Helper method to acquire an access token for a user using their username and password.
     *
     * @param username User's username (UPN).
     * @param password User's password.
     * @param scopes   Set of scopes required for the token.
     * @return An ID token or access token as a String.
     * @throws Exception If token acquisition fails.
     */
    private String acquireToken(String username, String password, Set<String> scopes) throws Exception {
        // Use the injected PublicClientApplication if available, otherwise create a new instance
        PublicClientApplication app = publicClientApp != null ? publicClientApp :
                PublicClientApplication.builder(clientId).authority(authority).build();

        UserNamePasswordParameters parameters = UserNamePasswordParameters.builder(scopes, username, password.toCharArray())
                .build();

        CompletableFuture<IAuthenticationResult> future = app.acquireToken(parameters);
        IAuthenticationResult result = future.get();

        // Return the ID token if needed; otherwise, access token can be used.
        return result.idToken();
    }

    /**
     * Helper method to acquire an access token using client credentials (for app-to-app scenarios).
     *
     * @param scopes Set of scopes required for the token.
     * @return An access token as a String.
     * @throws Exception If token acquisition fails.
     */
    public String acquireTokenWithClientCredentials(Set<String> scopes) throws Exception {
        // Using ConfidentialClientApplication for client-credential flow
        ConfidentialClientApplication app = ConfidentialClientApplication.builder(clientId,
                        ClientCredentialFactory.createFromSecret(clientSecret))
                .authority(authority)
                .build();

        ClientCredentialParameters parameters = ClientCredentialParameters.builder(scopes).build();
        CompletableFuture<IAuthenticationResult> future = app.acquireToken(parameters);
        IAuthenticationResult result = future.get();

        // Return the access token for app-to-app use cases.
        return result.accessToken();
    }
}
