package infrastructure.azure.ad;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.UsernamePasswordCredential;
import core.UserPool;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

public class AzureUserPoolTest {

    @Mock
    private ClientSecretCredential mockCredential;
    @Mock
    private UsernamePasswordCredential mockUsernamePasswordCredential;

    private AzureUserPool userPool;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // Initialize AzureUserPool with mock credentials
        userPool = new AzureUserPool("client_id", "tenant_id", "client_secret", mockCredential) {
            @Override
            protected UsernamePasswordCredential createUsernamePasswordCredential(String username, String password, String clientId) {
                return mockUsernamePasswordCredential;
            }
        };
    }

    @Test
    public void testVerify() {
        // Prepare a mock AccessToken
        AccessToken mockAccessToken = new AccessToken("mocked-azure-ad-token", OffsetDateTime.now().plusHours(1));

        // Simulate Azure AD token response for UsernamePasswordCredential
        Mockito.when(mockUsernamePasswordCredential.getToken(Mockito.any(TokenRequestContext.class)))
                .thenReturn(Mono.just(mockAccessToken));

        // Call the verify method on AzureUserPool
        String actualToken = userPool.verify("username", "password");

        // Verify the response
        Assert.assertNotNull(actualToken);
        Assert.assertEquals(actualToken, "mocked-azure-ad-token");
    }
}
