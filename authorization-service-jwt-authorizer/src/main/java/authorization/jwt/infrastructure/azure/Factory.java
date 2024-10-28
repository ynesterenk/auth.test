package authorization.jwt.infrastructure.azure;
import authorization.jwt.infrastructure.azure.rbac.AzureResourceManagerWrapper;
import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import authorization.jwt.core.Jwt;
import authorization.jwt.core.PolicyRepository;
import authorization.jwt.infrastructure.azure.rbac.RolePolicyRepository;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.Getter;
import lombok.SneakyThrows;

import java.net.URL;
import java.util.Map;

public class Factory {

    private static final String AZURE_TENANT_ID = "AZURE_AD_TENANT_ID";
    private static final String AZURE_SUBSCRIPTION_ID = "AZURE_AD_SUBSCRIPTION_ID";
    private static final String AZURE_AD_CLIENT_ID = "AZURE_AD_CLIENT_ID";

    @Getter(lazy = true)
    private static final Factory instance = new Factory(System.getenv());

    private final String azureTenantId;
    private final String azureSubscriptionId;
    private final JWKSource<? extends SecurityContext> jwkSource;
    private final AzureResourceManager azureResourceManager;

    @SneakyThrows
    public Factory(Map<String, String> env) {
        azureTenantId = env.get(AZURE_TENANT_ID);
        azureSubscriptionId = env.get(AZURE_SUBSCRIPTION_ID);
        // Set up JWKSource using Azure AD endpoint for JWT verification
        jwkSource = new RemoteJWKSet<>(new URL(String.format(
                "https://login.microsoftonline.com/%s/discovery/v2.0/keys", azureTenantId)));

        // Initialize TokenCredential
        TokenCredential credential = new DefaultAzureCredentialBuilder().build();

        // Create AzureProfile with your Azure environment and subscription
        AzureProfile profile = new AzureProfile(AZURE_TENANT_ID,azureSubscriptionId, AzureEnvironment.AZURE);

        // Authenticate with both TokenCredential and AzureProfile
        azureResourceManager = AzureResourceManager
                .authenticate(credential, profile)
                .withDefaultSubscription();
    }

    public Jwt createJsonWebToken() {
        return new Jwt(jwkSource);
    }

    public PolicyRepository createRolePolicyRepository() {
        return new RolePolicyRepository(new AzureResourceManagerWrapper(azureResourceManager));
    }

}
