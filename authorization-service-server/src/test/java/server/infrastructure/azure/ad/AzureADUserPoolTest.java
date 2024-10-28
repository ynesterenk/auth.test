package server.infrastructure.azure.ad;

import authorization.server.core.UserPoolException;
import authorization.server.infrastructure.azure.ad.AzureADUserPool;
import com.microsoft.aad.msal4j.*;
import org.mockito.*;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.naming.AuthenticationException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class AzureADUserPoolTest {

    @Mock
    private PublicClientApplication mockPublicClientApplication;
    @Mock
    private ConfidentialClientApplication mockConfidentialClientApplication;
    @Mock
    private IAuthenticationResult mockAuthenticationResult;
    @Captor
    private ArgumentCaptor<UserNamePasswordParameters> userPasswordParametersCaptor;
    @Captor
    private ArgumentCaptor<ClientCredentialParameters> clientCredentialParametersCaptor;

    private AzureADUserPool azureAdUserPool;

    private final String clientId = System.getenv("AZURE_CLIENT_ID");
    private final String clientSecret = System.getenv("AZURE_CLIENT_SECRET");
    private final String authority = "https://login.microsoftonline.com/your-tenant-id/";

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        azureAdUserPool = new AzureADUserPool(clientId, clientSecret, authority);
    }

    @Test
    public void testAuthenticate() throws Exception {
        String expectedToken = "id_token";

        // Set up the mock behavior for acquiring a token
        CompletableFuture<IAuthenticationResult> future = CompletableFuture.completedFuture(mockAuthenticationResult);
        Mockito.when(mockPublicClientApplication.acquireToken(Mockito.any(UserNamePasswordParameters.class))).thenReturn(future);
        Mockito.when(mockAuthenticationResult.idToken()).thenReturn(expectedToken);

        String actualToken = azureAdUserPool.authenticate("foo", "bar");

        Assert.assertNotNull(actualToken);
        Assert.assertEquals(actualToken, expectedToken);
        Mockito.verify(mockPublicClientApplication).acquireToken(userPasswordParametersCaptor.capture());
        UserNamePasswordParameters capturedParams = userPasswordParametersCaptor.getValue();
        Assert.assertEquals(capturedParams.username(), "foo");
    }

    @Test(expectedExceptions = UserPoolException.class, expectedExceptionsMessageRegExp = "Authentication failed for user .*")
    public void testAuthenticateFailure() throws Exception {
        // Simulate an authentication failure
        CompletableFuture<IAuthenticationResult> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new AuthenticationException("Failed to authenticate"));
        Mockito.when(mockPublicClientApplication.acquireToken(Mockito.any(UserNamePasswordParameters.class))).thenReturn(failedFuture);

        azureAdUserPool.authenticate("foo", "bar");
    }

    @Test
    public void testAcquireTokenWithClientCredentials() throws Exception {
        Set<String> scopes = Collections.singleton("https://graph.microsoft.com/.default");
        String expectedAccessToken = "access_token";

        // Set up the mock behavior for client credentials flow
        CompletableFuture<IAuthenticationResult> future = CompletableFuture.completedFuture(mockAuthenticationResult);
        Mockito.when(mockConfidentialClientApplication.acquireToken(Mockito.any(ClientCredentialParameters.class))).thenReturn(future);
        Mockito.when(mockAuthenticationResult.accessToken()).thenReturn(expectedAccessToken);

        String actualToken = azureAdUserPool.acquireTokenWithClientCredentials(scopes);

        Assert.assertNotNull(actualToken);
        Assert.assertEquals(actualToken, expectedAccessToken);
        Mockito.verify(mockConfidentialClientApplication).acquireToken(clientCredentialParametersCaptor.capture());
        ClientCredentialParameters capturedParams = clientCredentialParametersCaptor.getValue();
        Assert.assertEquals(capturedParams.scopes(), scopes);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class, expectedExceptionsMessageRegExp = "Azure AD requires self-service password reset or admin intervention to change password.")
    public void testChangePasswordUnsupported() throws UserPoolException {
        // Azure AD does not support changing passwords directly through MSAL
        azureAdUserPool.changePassword("foo", "oldPass", "newPass");
    }

    @DataProvider
    public Object[][] authenticationExceptionSamples() {
        return new Object[][]{
                {ExecutionException.class}, {InterruptedException.class}
        };
    }

    @Test(dataProvider = "authenticationExceptionSamples", expectedExceptions = UserPoolException.class)
    public void testAuthenticateWithExceptions(Class<? extends Exception> exceptionClass) throws Exception {
        CompletableFuture<IAuthenticationResult> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(exceptionClass.getConstructor(String.class).newInstance("Simulated exception"));
        Mockito.when(mockPublicClientApplication.acquireToken(Mockito.any(UserNamePasswordParameters.class))).thenReturn(failedFuture);

        azureAdUserPool.authenticate("foo", "bar");
    }
}
