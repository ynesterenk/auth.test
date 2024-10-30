package server.core.facade;

import authorization.server.core.facade.ClientCredentialsFacade;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import authorization.server.core.UserPool;
import authorization.server.core.UserPoolException;
import authorization.server.core.model.ClientCredentialsRequest;
import authorization.server.core.model.ClientCredentialsResponse;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import lombok.extern.slf4j.Slf4j;
import shared.core.validation.ErrorState;
import shared.core.validation.Rule;
import shared.infrastructure.azure.gateway.JsonUtil;
import shared.infrastructure.azure.gateway.proxy.HttpHeaders;
import shared.infrastructure.azure.gateway.proxy.HttpRequestTranslator;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

import static server.TestHelper.SetupMockResponse;

@Slf4j
public class ClientCredentialsFacadeTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserPool mockUserPool;
    @Spy
    private ErrorState spyErrorState;
    @Mock
    private Collection<Rule<HttpRequestTranslator>> mockPreRules;
    @Mock
    private Collection<Rule<ClientCredentialsRequest>> mockPostRules;

    private ClientCredentialsFacade facade;

    @BeforeMethod
    public void setUp() throws UserPoolException {
        MockitoAnnotations.initMocks(this);
        facade = new ClientCredentialsFacade(
                spyErrorState, mockUserPool, mockPreRules, mockPostRules);
    }

    @Test
    public void testAuthenticate() throws UserPoolException, JsonProcessingException {
        HttpRequestTranslator request = makeHttpRequestTranslator(makeClientCredentialsRequest());
        Mockito.when(mockUserPool.authenticate("foo", "bar")).thenReturn("ACCESS_TOKEN");


// Call facade.process with HttpRequestTranslator
        HttpResponseMessage actual = facade.process(request);

        // Assert the response values
        Assert.assertNotNull(actual);
        Assert.assertEquals(actual.getStatusCode(), HttpStatus.OK.value());
        Assert.assertNotNull(actual.getBody());
        try {
            // Deserialize the response body to verify token details
            ClientCredentialsResponse response = objectMapper.readValue(actual.getBody().toString(), ClientCredentialsResponse.class);
            Assert.assertEquals(response.getAccessToken(), "ACCESS_TOKEN");
            Assert.assertEquals(response.getTokenType(), "Bearer");
            Assert.assertTrue(Math.abs(response.getExpiresIn() - Instant.now().getEpochSecond()) <= 3600);
        }catch (java.io.IOException e)
        {
            log.info(e.getMessage());
        }
    }

    @Test
    public void testAuthorized() throws UserPoolException {
        Mockito.when(mockUserPool.authenticate(Mockito.any(), Mockito.any()))
                .thenReturn("foo");

        HttpRequestTranslator request = makeHttpRequestTranslator(HttpStatus.OK);
        HttpResponseMessage actual = facade.process(request);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual.getStatusCode(), HttpStatus.OK.value());
        Assert.assertNotNull(actual.getBody());
    }

    @Test
    public void testUnauthorized() throws UserPoolException {
        Mockito.when(mockUserPool.authenticate("foo", "bar"))
                .thenThrow(UserPoolException.class);

        HttpRequestTranslator request = makeHttpRequestTranslator(HttpStatus.UNAUTHORIZED);
        HttpResponseMessage actual = facade.process(request);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual.getStatusCode(), HttpStatus.UNAUTHORIZED.value());
        Assert.assertNotNull(actual.getBody());
    }

    private static HttpRequestTranslator makeHttpRequestTranslator(ClientCredentialsRequest clientCredentialsRequest) {
        // Mock HttpRequestMessage to create an HttpRequestTranslator
        HttpRequestMessage<Optional<String>> mockRequest = Mockito.mock(HttpRequestMessage.class);

        SetupMockResponse(mockRequest, HttpStatus.OK);
        // Convert ClientCredentialsRequest to JSON
        String requestBody = JsonUtil.toJsonString(clientCredentialsRequest);

        // Set method and body for the request
        Mockito.when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of(requestBody));

        // Set headers
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        Mockito.when(mockRequest.getHeaders()).thenReturn(headers);

        return new HttpRequestTranslator(mockRequest);
    }

    private static ClientCredentialsRequest makeClientCredentialsRequest() {
        ClientCredentialsRequest request = new ClientCredentialsRequest();
        request.setGrantType("client_credentials");
        request.setClientId("foo");
        request.setClientSecret("bar");
        return request;
    }


    private static HttpRequestTranslator makeHttpRequestTranslator(HttpStatus httpStatus) {
        // Mock HttpRequestMessage to create an HttpRequestTranslator
        HttpRequestMessage<Optional<String>> mockRequest = Mockito.mock(HttpRequestMessage.class);


        SetupMockResponse(mockRequest, httpStatus);
        // Set method and body for the request
        Mockito.when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of(JsonUtil.toJsonString(makeClientCredentialsRequest())));

        // Set headers
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        Mockito.when(mockRequest.getHeaders()).thenReturn(headers);

        return new HttpRequestTranslator(mockRequest);
    }
}
