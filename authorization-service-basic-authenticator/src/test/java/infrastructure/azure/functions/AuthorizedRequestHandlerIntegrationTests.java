package infrastructure.azure.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import core.Principal;
import infrastructure.azure.AzureAdClientFactory;
import infrastructure.azure.AzureFactory;
import org.junit.Ignore;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import shared.infrastructure.azure.gateway.AuthorizerRequest;
import shared.infrastructure.azure.gateway.MockHttpRequestMessage;


import java.util.*;

import static shared.infrastructure.azure.gateway.MockHttpRequestMessage.GetBearerToken;

public class AuthorizedRequestHandlerIntegrationTests {

    private AzureFactory azureFactory;
    private AuthorizerRequestHandler handler;
    private HttpRequestMessage<Optional<AuthorizerRequest>> authRequest;
    private AuthorizerRequest request;
    private Principal principal;
    private ExecutionContext execContext;

    @BeforeMethod
    public void setUp() {

        Map<String, String> conf = new HashMap<>();
        conf.put("AZURE_TENANT_ID", System.getenv("AZURE_TENANT_ID"));
        conf.put("AZURE_CLIENT_ID", System.getenv("AZURE_CLIENT_ID"));
        conf.put("AZURE_CLIENT_SECRET", System.getenv("AZURE_CLIENT_SECRET"));
        AzureAdClientFactory azureAdClientFactory = new AzureAdClientFactory(conf.get("AZURE_TENANT_ID"));
        // Initialize AzureFactory with the credentials
        azureFactory = new AzureFactory(azureAdClientFactory, conf);

        // Initialize AuthorizerRequestHandler
        handler = new AuthorizerRequestHandler(azureFactory);

        // Initialize other required objects
        request = makeRequest();
        principal = makePrincipal();
        authRequest = new MockHttpRequestMessage(request); // Implement this with real request
    }

    //@Test
    @Ignore("This is integration test. It will only work for Microsoft work or school account" +
            " if RBAC flow is enabled allowing user/password authentication")
    public void testAuthentication() throws Exception {
        // Run the handler
        HttpResponseMessage response = handler.run(authRequest);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatus(), HttpStatus.OK);

        // Parse and verify the JSON response body
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseBody = objectMapper.readTree((String) response.getBody());

        Assert.assertEquals(responseBody.get("principalId").asText(), principal.getId());
        Assert.assertEquals(responseBody.get("context").get("username").asText(), principal.getUsername());
        Assert.assertEquals(responseBody.get("context").get("scope").asText(), "User.Read,Files.Read");
        Assert.assertEquals(responseBody.get("context").get("expirationTime").asLong(), 1234567890L);
    }

    private static AuthorizerRequest makeRequest() {
        AuthorizerRequest request = new AuthorizerRequest();
        request.setMethodArn("arn:azure:::"); // Adjusted for Azure
        request.setAuthorizationToken(GetBearerToken()); // Provide a real token if possible
        return request;
    }




    private static Principal makePrincipal() {
        Principal principal = new Principal();
        principal.setId("32944624-1f4a-4f34-bdf6-5450679ef1bf");
        principal.setUsername("admin");
        principal.setScope(Arrays.asList("User.Read", "Files.Read"));
        principal.setExpirationTime(1234567890L);
        return principal;
    }




}
