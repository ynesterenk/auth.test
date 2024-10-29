package infrastructure.azure.ad;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.UsernamePasswordCredential;
import com.azure.identity.UsernamePasswordCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
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
        String token = authenticateWithAzureAd(username, password, clientId);

        return token;
    }

    private String authenticateWithAzureAd(String username, String password, String appClientId) {
        // Create a UsernamePasswordCredential using the helper method
        UsernamePasswordCredential usernamePasswordCredential = createUsernamePasswordCredential(username, password, appClientId);
       // Define the scope for the token (example: Azure Key Vault scope)
        TokenRequestContext tokenRequestContext = new TokenRequestContext()
                .addScopes("https://vault.azure.net/.default");

        // Retrieve the token
        AccessToken accessToken = usernamePasswordCredential.getToken(tokenRequestContext).block();
        return accessToken.getToken();
    }

    // Helper method to create a UsernamePasswordCredential
    protected UsernamePasswordCredential createUsernamePasswordCredential(String username, String password, String clientId) {
        return new UsernamePasswordCredentialBuilder()
                .clientId(clientId)
                .username(username)
                .password(password)
                .build();
    }
}
