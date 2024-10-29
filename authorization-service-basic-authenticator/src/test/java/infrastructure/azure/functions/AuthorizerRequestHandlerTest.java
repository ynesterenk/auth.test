package infrastructure.azure.functions;

import com.azure.core.credential.TokenCredential;
import com.microsoft.graph.requests.AppRoleAssignmentCollectionPage;
import core.HttpBasicAuthenticator;
import core.Principal;
import infrastructure.azure.AzureFactory;
import shared.core.http.BasicAuthenticationException;
import shared.infrastructure.azure.gateway.AuthorizerRequest;
import shared.infrastructure.azure.gateway.AuthorizerResponse;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.AppRoleAssignment;
import com.microsoft.graph.requests.GraphServiceClient;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class AuthorizerRequestHandlerTest {

    @Mock
    private AzureFactory mockFactory;
    @Mock
    private HttpBasicAuthenticator mockHttpBasicAuthenticator;
    @Mock
    private HttpRequestMessage<Optional<AuthorizerRequest>> mockRequest;
    @Mock
    private ExecutionContext mockContext;
    @Mock
    private TokenCredentialAuthProvider mockAuthProvider;
    @Mock
    private GraphServiceClient<?> mockGraphClient;
    @Mock
    private TokenCredential mockTokenCredential;

    private AuthorizerRequestHandler handler;
    private AuthorizerRequest request;
    private Principal principal;

    private static final String REQUIRED_ROLE_ID = "your-required-role-id"; // Define the required role ID here

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // Configure factory to return a mock authenticator and graph client
        Mockito.when(mockFactory.createHttpBasicAuthenticator()).thenReturn(mockHttpBasicAuthenticator);
        // Configure factory to return a mock TokenCredential
        Mockito.when(mockFactory.getAzureIdentityClient()).thenReturn(mockTokenCredential);

        handler = new AuthorizerRequestHandler(mockFactory);
        request = makeRequest();
        principal = makePrincipal();
    }

    @Test
    public void testAuthentication() {
        Mockito.when(mockHttpBasicAuthenticator.authenticate(Mockito.anyString())).thenReturn(principal);
        Mockito.when(mockRequest.getHeaders()).thenReturn(Collections.singletonMap("Authorization", "Bearer mock_token"));
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of(request));

        // Simulate required role assignment in Azure AD
        AppRoleAssignment requiredRoleAssignment = new AppRoleAssignment();
        requiredRoleAssignment.appRoleId = UUID.fromString(REQUIRED_ROLE_ID);
        requiredRoleAssignment.principalId = UUID.fromString(principal.getId());

        // Create a mock AppRoleAssignmentCollectionPage
        AppRoleAssignmentCollectionPage mockRoleAssignmentsPage = Mockito.mock(AppRoleAssignmentCollectionPage.class);
        Mockito.when(mockRoleAssignmentsPage.getCurrentPage()).thenReturn(Collections.singletonList(requiredRoleAssignment));

        // Mock Graph Service Client to return the mock AppRoleAssignmentCollectionPage
        Mockito.when(mockGraphClient.users(principal.getId()).appRoleAssignments().buildRequest().get())
                .thenReturn(mockRoleAssignmentsPage);

        HttpResponseMessage response = handler.run(mockRequest, mockContext);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatus(), HttpStatus.OK);

        AuthorizerResponse actual = (AuthorizerResponse) response.getBody();
        Assert.assertEquals(actual.getPrincipalId(), principal.getId());
        Assert.assertEquals(actual.getContext().get("username"), principal.getUsername());
        Assert.assertEquals(actual.getContext().get("scope"), "foo,bar");
        Assert.assertEquals(actual.getContext().get("expirationTime"), 1234567890L);
    }

    @Test
    public void testUnauthorized() {
        Mockito.when(mockHttpBasicAuthenticator.authenticate(Mockito.anyString()))
                .thenThrow(BasicAuthenticationException.class);
        Mockito.when(mockRequest.getHeaders()).thenReturn(Collections.singletonMap("Authorization", "Bearer mock_token"));
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of(request));

        HttpResponseMessage response = handler.run(mockRequest, mockContext);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatus(), HttpStatus.UNAUTHORIZED);
        Assert.assertEquals(response.getBody(), "Unauthorized: Basic authentication failed");
    }


    @Test
    public void testUserWithoutRequiredRole() {
        Mockito.when(mockHttpBasicAuthenticator.authenticate(Mockito.anyString())).thenReturn(principal);
        Mockito.when(mockRequest.getHeaders()).thenReturn(Collections.singletonMap("Authorization", "Bearer mock_token"));
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of(request));

        // Create a mock AppRoleAssignmentCollectionPage and configure it to return an empty list for getCurrentPage()
        AppRoleAssignmentCollectionPage emptyRoleAssignmentsPage = Mockito.mock(AppRoleAssignmentCollectionPage.class);
        Mockito.when(emptyRoleAssignmentsPage.getCurrentPage()).thenReturn(Collections.emptyList());

        // Configure the mock GraphServiceClient to return the empty role assignments page
        Mockito.when(mockGraphClient.users(principal.getId()).appRoleAssignments().buildRequest().get())
                .thenReturn(emptyRoleAssignmentsPage);

        HttpResponseMessage response = handler.run(mockRequest, mockContext);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatus(), HttpStatus.FORBIDDEN);
        Assert.assertEquals(response.getBody(), "User does not have the required role");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testInternalServerError() {
        Mockito.when(mockHttpBasicAuthenticator.authenticate(Mockito.anyString())).thenThrow(RuntimeException.class);
        Mockito.when(mockRequest.getHeaders()).thenReturn(Collections.singletonMap("Authorization", "Bearer mock_token"));
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of(request));

        handler.run(mockRequest, mockContext);
    }

    private static AuthorizerRequest makeRequest() {
        AuthorizerRequest request = new AuthorizerRequest();
        request.setMethodArn("arn:azure:::"); // Adjusted for Azure
        request.setAuthorizationToken("Bearer mock_token");
        return request;
    }

    private static Principal makePrincipal() {
        Principal principal = new Principal();
        principal.setId("32944624-1f4a-4f34-bdf6-5450679ef1bf");
        principal.setUsername("admin");
        principal.setScope(Arrays.asList("foo", "bar"));
        principal.setExpirationTime(1234567890L);
        return principal;
    }
}
