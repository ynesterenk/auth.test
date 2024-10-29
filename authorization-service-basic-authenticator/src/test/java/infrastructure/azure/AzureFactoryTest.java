package infrastructure.azure;


import core.HttpBasicAuthenticator;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredential;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

public class AzureFactoryTest {

    @Mock
    private AzureAdClientFactory mockAzureAdClientFactory;
    @Mock
    private ClientSecretCredential mockTokenCredential; // Change mock type to ClientSecretCredential

    private AzureFactory azureFactory;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // Configure mock behavior with cast to ClientSecretCredential
        Mockito.when(mockAzureAdClientFactory.getAzureIdentityClient()).thenReturn(mockTokenCredential);

        // Initialize AzureFactory with mocked AzureAdClientFactory and configuration map
        azureFactory = new AzureFactory(mockAzureAdClientFactory, Map.of(
                "AZURE_TENANT_ID", "test-tenant-id",
                "AZURE_CLIENT_ID", "test-client-id",
                "AZURE_CLIENT_SECRET", "test-client-secret"
        ));
    }

    @Test
    public void testCreateHttpBasicAuthenticator() {
        HttpBasicAuthenticator actual = azureFactory.createHttpBasicAuthenticator();

        Assert.assertNotNull(actual);
    }

    @Test
    public void testGetAzureIdentityClient() {
        TokenCredential actual = azureFactory.getAzureIdentityClient();

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual, mockTokenCredential);
        Mockito.verify(mockAzureAdClientFactory).getAzureIdentityClient();
    }
}