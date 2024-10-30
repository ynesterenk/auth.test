package infrastructure.azure;

import com.azure.core.credential.TokenCredential;
import core.HttpBasicAuthenticator;
import core.UserPool;
import infrastructure.azure.ad.AzureUserPool;
import lombok.Getter;

import java.util.Map;

public class AzureFactory {

    @Getter(lazy = true)
    private static final AzureFactory instance = new AzureFactory();

    private final AzureAdClientFactory azureFactory;
    private final String tenantId;
    private final String clientId;
    private final String clientSecret;

    private AzureFactory() {
        this(new AzureAdClientFactory(System.getenv("AZURE_TENANT_ID")), System.getenv());
    }

    public AzureFactory(AzureAdClientFactory azureFactory, Map<String, String> conf) {
        this.azureFactory = azureFactory;
        this.tenantId = conf.get("AZURE_TENANT_ID");
        this.clientId = conf.get("AZURE_CLIENT_ID");
        this.clientSecret = conf.get("AZURE_CLIENT_SECRET");
    }

    public HttpBasicAuthenticator createHttpBasicAuthenticator() {
        return new HttpBasicAuthenticator(createAzureUserPool());
    }

    private UserPool createAzureUserPool() {
        return new AzureUserPool(clientId, tenantId, clientSecret, azureFactory.getAzureIdentityClient());
    }

    public TokenCredential getAzureIdentityClient() {

        return azureFactory.getAzureIdentityClient();
    }
}
