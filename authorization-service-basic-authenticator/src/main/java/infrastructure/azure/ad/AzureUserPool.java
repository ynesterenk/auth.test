package infrastructure.azure.ad;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import core.UserPool;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

@RequiredArgsConstructor
public class AzureUserPool implements UserPool {

    private static final Logger logger = LoggerFactory.getLogger(AzureUserPool.class);

    private final String clientId;
    private final String tenantId;
    private final String clientSecret;
    private final ClientSecretCredential credential;

    @Override
    public String verify(String username, String password) {
        logger.info("Verifying user {}", username);

        // Implement authentication with Azure AD using `ClientSecretCredential`
        // Create request with Azure AD endpoint for token retrieval
        String token = authenticateWithAzureAd(username, password);

        return token;
    }

    private String authenticateWithAzureAd(String username, String password) {
        // Azure AD Authentication Logic with provided username/password
        // This typically involves calling the MS Graph API for token generation
        // Here, you would construct and send a request for the token and parse the response

        return "azure-ad-id-token"; // placeholder for token
    }
}
