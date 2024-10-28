package infrastructure.azure;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class AzureAdClientFactory {

    private final String tenantId;
    private final String clientId;
    private final String clientSecret;

    @Getter(lazy = true)
    private final ClientSecretCredential azureIdentityClient = createClientSecretCredential();

    AzureAdClientFactory(String tenantId) {
        this(tenantId, System.getenv("AZURE_CLIENT_ID"), System.getenv("AZURE_CLIENT_SECRET"));
    }

    private ClientSecretCredential createClientSecretCredential() {
        return new ClientSecretCredentialBuilder()
                .tenantId(tenantId)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }
}
