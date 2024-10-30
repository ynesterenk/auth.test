package infrastructure.azure.functions;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.graph.requests.*;
import core.HttpBasicAuthenticator;
import core.Principal;
import infrastructure.azure.AzureFactory;
import reactor.core.publisher.Mono;
import shared.core.http.BasicAuthenticationException;
import shared.infrastructure.azure.gateway.AuthorizerRequest;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.AppRoleAssignment;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
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

        // Configure factory to return a mock TokenCredential
        Mockito.when(mockFactory.getAzureIdentityClient()).thenReturn(mockTokenCredential);

        handler = new AuthorizerRequestHandler(mockFactory);
        request = makeRequest();
        principal = makePrincipal();
    }



    @Test
    public void testAuthentication() throws JsonProcessingException {
        MockitoAnnotations.initMocks(this);

        // Configure mock TokenCredential to return a valid Mono<AccessToken>
        AccessToken mockAccessToken = new AccessToken("mocked-azure-ad-token", OffsetDateTime.now().plusHours(1));
        Mockito.when(mockTokenCredential.getToken(Mockito.any(TokenRequestContext.class)))
                .thenReturn(Mono.just(mockAccessToken));
        String jsonBody = "{"
                + "\"principalId\": \"" + principal.getId() + "\","
                + "\"context\": {"
                + "    \"username\": \"" + principal.getUsername() + "\","
                + "    \"scope\": \"foo,bar\","
                + "    \"expirationTime\": 1234567890"
                + "}"
                + "}";
        // Mock the behavior of HttpRequestMessage.createResponseBuilder
        HttpResponseMessage.Builder mockResponseBuilder = Mockito.mock(HttpResponseMessage.Builder.class);
        HttpResponseMessage mockResponse = Mockito.mock(HttpResponseMessage.class);
        Mockito.when(mockResponseBuilder.body(Mockito.any())).thenReturn(mockResponseBuilder);
        Mockito.when(mockResponseBuilder.status(Mockito.any())).thenReturn(mockResponseBuilder);
        Mockito.when(mockResponseBuilder.build()).thenReturn(mockResponse);
        Mockito.when(mockResponse.getStatus()).thenReturn(HttpStatus.OK);
        Mockito.when(mockRequest.createResponseBuilder(Mockito.any(HttpStatus.class))).thenReturn(mockResponseBuilder);
        Mockito.when(mockResponse.getBody()).thenReturn(jsonBody);

        // Set up other mocks for the Graph API client and request chain
        Mockito.when(mockHttpBasicAuthenticator.authenticate(Mockito.anyString())).thenReturn(principal);
        Mockito.when(mockRequest.getHeaders()).thenReturn(Collections.singletonMap("Authorization", "Bearer mock_token"));
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of(request));

        AppRoleAssignment requiredRoleAssignment = new AppRoleAssignment();
        requiredRoleAssignment.appRoleId = UUID.fromString("a0f1c3b4-67e8-49c2-8f1e-d3f8a5b7c8d9");
        requiredRoleAssignment.principalId = UUID.fromString(principal.getId());

        AppRoleAssignmentCollectionPage mockRoleAssignmentsPage = Mockito.mock(AppRoleAssignmentCollectionPage.class);
        Mockito.when(mockRoleAssignmentsPage.getCurrentPage()).thenReturn(Collections.singletonList(requiredRoleAssignment));

        UserRequestBuilder mockUserRequestBuilder = Mockito.mock(UserRequestBuilder.class);
        AppRoleAssignmentCollectionRequestBuilder mockAppRoleAssignmentRequestBuilder = Mockito.mock(AppRoleAssignmentCollectionRequestBuilder.class);
        AppRoleAssignmentCollectionRequest mockAppRoleAssignmentRequest = Mockito.mock(AppRoleAssignmentCollectionRequest.class);

        Mockito.when(mockGraphClient.users(principal.getId())).thenReturn(mockUserRequestBuilder);
        Mockito.when(mockUserRequestBuilder.appRoleAssignments()).thenReturn(mockAppRoleAssignmentRequestBuilder);
        Mockito.when(mockAppRoleAssignmentRequestBuilder.buildRequest()).thenReturn(mockAppRoleAssignmentRequest);
        Mockito.when(mockAppRoleAssignmentRequest.get()).thenReturn(mockRoleAssignmentsPage);
       // Configure factory to return a mock authenticator and graph client
        Mockito.when(mockFactory.createHttpBasicAuthenticator()).thenReturn(mockHttpBasicAuthenticator);


        // Run the handler
        HttpResponseMessage response = handler.run(mockRequest);

        Assert.assertNotNull(response);
      // Parse and verify the JSON response body
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseBody = objectMapper.readTree((String) response.getBody());

        Assert.assertEquals(responseBody.get("principalId").asText(), principal.getId());
        Assert.assertEquals(responseBody.get("context").get("username").asText(), principal.getUsername());
        Assert.assertEquals(responseBody.get("context").get("scope").asText(), "foo,bar");
        Assert.assertEquals(responseBody.get("context").get("expirationTime").asLong(), 1234567890L);
    }



    @Test
    public void testUnauthorized() {
        Mockito.when(mockHttpBasicAuthenticator.authenticate(Mockito.anyString()))
                .thenThrow(BasicAuthenticationException.class);
        HttpResponseMessage.Builder mockResponseBuilder = Mockito.mock(HttpResponseMessage.Builder.class);
        HttpResponseMessage mockResponse = Mockito.mock(HttpResponseMessage.class);
        Mockito.when(mockResponseBuilder.body(Mockito.any())).thenReturn(mockResponseBuilder);
        Mockito.when(mockResponseBuilder.status(Mockito.any())).thenReturn(mockResponseBuilder);
        Mockito.when(mockResponseBuilder.build()).thenReturn(mockResponse);
        Mockito.when(mockResponse.getStatus()).thenReturn(HttpStatus.UNAUTHORIZED);
        Mockito.when(mockRequest.createResponseBuilder(Mockito.any(HttpStatus.class))).thenReturn(mockResponseBuilder);
        Mockito.when(mockRequest.getHeaders()).thenReturn(Collections.singletonMap("Authorization", "Bearer mock_token"));
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of(request));
        Mockito.when(mockFactory.createHttpBasicAuthenticator()).thenReturn(mockHttpBasicAuthenticator);
        Mockito.when(mockResponse.getBody()).thenReturn("Unauthorized: Basic authentication failed");

        HttpResponseMessage response = handler.run(mockRequest);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatus(), HttpStatus.UNAUTHORIZED);
        Assert.assertEquals(response.getBody(), "Unauthorized: Basic authentication failed");
    }


    @Test
    public void testUserWithoutRequiredRole() {
        Mockito.when(mockHttpBasicAuthenticator.authenticate(Mockito.anyString())).thenReturn(principal);
        Mockito.when(mockRequest.getHeaders()).thenReturn(Collections.singletonMap("Authorization", "Bearer mock_token"));
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of(request));
        HttpResponseMessage.Builder mockResponseBuilder = Mockito.mock(HttpResponseMessage.Builder.class);
        HttpResponseMessage mockResponse = Mockito.mock(HttpResponseMessage.class);
        Mockito.when(mockResponseBuilder.body(Mockito.any())).thenReturn(mockResponseBuilder);
        Mockito.when(mockResponseBuilder.status(Mockito.any())).thenReturn(mockResponseBuilder);
        Mockito.when(mockResponseBuilder.build()).thenReturn(mockResponse);
        Mockito.when(mockResponse.getStatus()).thenReturn(HttpStatus.FORBIDDEN);
        Mockito.when(mockRequest.createResponseBuilder(Mockito.any(HttpStatus.class))).thenReturn(mockResponseBuilder);
        Mockito.when(mockRequest.getHeaders()).thenReturn(Collections.singletonMap("Authorization", "Bearer mock_token"));
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of(request));
        Mockito.when(mockFactory.createHttpBasicAuthenticator()).thenReturn(mockHttpBasicAuthenticator);
        Mockito.when(mockResponse.getBody()).thenReturn("User does not have the required role");
        UserRequestBuilder mockUserRequestBuilder = Mockito.mock(UserRequestBuilder.class);
        AppRoleAssignmentCollectionRequestBuilder mockAppRoleAssignmentRequestBuilder = Mockito.mock(AppRoleAssignmentCollectionRequestBuilder.class);
        AppRoleAssignmentCollectionRequest mockAppRoleAssignmentRequest = Mockito.mock(AppRoleAssignmentCollectionRequest.class);
        AppRoleAssignmentCollectionPage mockRoleAssignmentsPage = Mockito.mock(AppRoleAssignmentCollectionPage.class);
        Mockito.when(mockGraphClient.users(principal.getId())).thenReturn(mockUserRequestBuilder);
        Mockito.when(mockUserRequestBuilder.appRoleAssignments()).thenReturn(mockAppRoleAssignmentRequestBuilder);
        Mockito.when(mockAppRoleAssignmentRequestBuilder.buildRequest()).thenReturn(mockAppRoleAssignmentRequest);
        Mockito.when(mockAppRoleAssignmentRequest.get()).thenReturn(mockRoleAssignmentsPage);
        // Create a mock AppRoleAssignmentCollectionPage and configure it to return an empty list for getCurrentPage()
        AppRoleAssignmentCollectionPage emptyRoleAssignmentsPage = Mockito.mock(AppRoleAssignmentCollectionPage.class);
        Mockito.when(emptyRoleAssignmentsPage.getCurrentPage()).thenReturn(Collections.emptyList());

        // Configure the mock GraphServiceClient to return the empty role assignments page
        Mockito.when(mockGraphClient.users(principal.getId()).appRoleAssignments().buildRequest().get())
                .thenReturn(emptyRoleAssignmentsPage);
        Mockito.when(mockFactory.createHttpBasicAuthenticator()).thenReturn(mockHttpBasicAuthenticator);

        HttpResponseMessage response = handler.run(mockRequest);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatus(), HttpStatus.FORBIDDEN);
        Assert.assertEquals(response.getBody(), "User does not have the required role");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testInternalServerError() {
        Mockito.when(mockHttpBasicAuthenticator.authenticate(Mockito.anyString())).thenThrow(RuntimeException.class);
        Mockito.when(mockRequest.getHeaders()).thenReturn(Collections.singletonMap("Authorization", "Bearer mock_token"));
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of(request));

        handler.run(mockRequest);
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
