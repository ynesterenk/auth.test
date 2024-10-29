
package infrastructure.azure;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AzureAdClientFactoryTest {

    @Mock
    private ClientSecretCredentialBuilder mockCredentialBuilder;
    @Mock
    private ClientSecretCredential mockClientSecretCredential;

    private AzureAdClientFactory azureAdClientFactory;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // Configure mock builder to return mock credential
        Mockito.when(mockCredentialBuilder.tenantId(Mockito.anyString())).thenReturn(mockCredentialBuilder);
        Mockito.when(mockCredentialBuilder.clientId(Mockito.anyString())).thenReturn(mockCredentialBuilder);
        Mockito.when(mockCredentialBuilder.clientSecret(Mockito.anyString())).thenReturn(mockCredentialBuilder);
        Mockito.when(mockCredentialBuilder.build()).thenReturn(mockClientSecretCredential);

        // Initialize AzureAdClientFactory with test values
        azureAdClientFactory = new AzureAdClientFactory("test-tenant-id", "test-client-id", "test-client-secret") {
            @Override
            protected ClientSecretCredentialBuilder createClientSecretCredentialBuilder() {
                return mockCredentialBuilder;
            }
        };
    }

    @Test
    public void testCreateClientSecretCredential() {
        // Call method under test
        ClientSecretCredential actual = azureAdClientFactory.getAzureIdentityClient();

        // Assert the credential is correctly retrieved
        Assert.assertNotNull(actual);
        Assert.assertEquals(actual, mockClientSecretCredential);
        Mockito.verify(mockCredentialBuilder).tenantId("test-tenant-id");
        Mockito.verify(mockCredentialBuilder).clientId("test-client-id");
        Mockito.verify(mockCredentialBuilder).clientSecret("test-client-secret");
        Mockito.verify(mockCredentialBuilder).build();
    }
}
