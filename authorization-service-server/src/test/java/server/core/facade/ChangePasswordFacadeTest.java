package server.core.facade;

import authorization.server.core.facade.ChangePasswordFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import authorization.server.core.UserPool;
import authorization.server.core.UserPoolException;
import authorization.server.core.model.ChangePasswordRequest;
import authorization.server.core.model.ChangePasswordResponse;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import lombok.extern.slf4j.Slf4j;
import shared.core.validation.ErrorState;
import shared.core.validation.Rule;
import shared.infrastructure.azure.gateway.proxy.HttpHeaders;
import shared.infrastructure.azure.gateway.proxy.HttpRequestTranslator;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.apache.velocity.Template;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.StringWriter;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

@Slf4j
public class ChangePasswordFacadeTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserPool mockUserPool;
    @Mock
    private Template mockTemplate;
    @Spy
    private ErrorState spyErrorState;
    @Mock
    private Collection<Rule<HttpRequestTranslator>> mockPreRules;
    @Mock
    private Collection<Rule<ChangePasswordRequest>> mockPostRules;

    private ChangePasswordFacade facade;

    @BeforeMethod
    public void setUp() throws UserPoolException {
        MockitoAnnotations.initMocks(this);
        facade = new ChangePasswordFacade(
                spyErrorState, mockUserPool, mockTemplate, mockPreRules, mockPostRules);
    }

    @Test
    public void testInvokeGetMethod() throws Exception {
        HttpRequestTranslator request = makeHttpRequestTranslator("GET", true, "text/html; charset=utf-8","foo bar" );
        Mockito.doAnswer(o -> {
            o.<StringWriter>getArgument(1).append("foo bar");
            return o;
        }).when(mockTemplate).merge(Mockito.any(), Mockito.any());

        HttpResponseMessage actual = facade.process(request);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual.getStatusCode(), HttpStatus.OK.value());
        Assert.assertEquals(actual.getHeader(HttpHeaders.CONTENT_TYPE), "text/html; charset=utf-8");
        Assert.assertEquals(actual.getBody().toString(), "foo bar");
    }

    @Test
    public void testSuccessChangePassword() throws Exception {
        HttpRequestTranslator request = makeHttpRequestTranslator("POST", true, "application/json","");

        Mockito.doNothing()
                .when(mockUserPool).changePassword(Mockito.eq("admin"), Mockito.eq("foo"), Mockito.eq("bar"));

        HttpResponseMessage actual = facade.process(request);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual.getStatusCode(), HttpStatus.OK.value());
        Assert.assertEquals(actual.getHeader(HttpHeaders.CONTENT_TYPE), "application/json");

        ChangePasswordResponse response = objectMapper.readValue(actual.getBody().toString(), ChangePasswordResponse.class);
        Assert.assertTrue(response.getAcknowledged());
        Assert.assertTrue(response.getMessage().contains("successfully"));
    }

    @Test
    public void testFailChangePassword() throws Exception {
        HttpRequestTranslator request = makeHttpRequestTranslator("POST",false, "application/json", "");

        Mockito.doThrow(new UserPoolException("foo"))
                .when(mockUserPool).changePassword(Mockito.eq("admin"), Mockito.eq("foo"), Mockito.eq("bar"));
        log.info("Facade value: "+facade.toString());
        HttpResponseMessage actual = facade.process(request);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual.getStatusCode(), HttpStatus.OK.value());
        Assert.assertEquals(actual.getHeader(HttpHeaders.CONTENT_TYPE), "application/json");

        ChangePasswordResponse response = objectMapper.readValue(actual.getBody().toString(), ChangePasswordResponse.class);
        Assert.assertFalse(response.getAcknowledged());
        Assert.assertFalse(response.getMessage().contains("successfully"));
    }



    private static HttpRequestTranslator makeHttpRequestTranslator(String httpMethod, Boolean acknowledge, String contentType, String inputBody) {
        HttpRequestMessage<Optional<String>> mockRequest = Mockito.mock(HttpRequestMessage.class);
        HttpResponseMessage.Builder mockBuilder = Mockito.mock(HttpResponseMessage.Builder.class);
        HttpResponseMessage mockResponse = Mockito.mock(HttpResponseMessage.class);
        Mockito.when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK.value());
        Mockito.when(mockResponse.getHeader(HttpHeaders.CONTENT_TYPE)).thenReturn(contentType);
        String jsonBody;
        if(acknowledge)
           jsonBody = "{\"acknowledged\": true, \"message\": \"Password change successfully\"}";
        else
            jsonBody = "{\"acknowledged\": false, \"message\": \"Password change failed\"}";
        if (inputBody.isEmpty())
           Mockito.when(mockResponse.getBody()).thenReturn(jsonBody);
        else
            Mockito.when(mockResponse.getBody()).thenReturn(inputBody);

        // Set up the request body and headers
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of("username=admin&previous_password=foo&proposed_password=bar"));
        Mockito.when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.valueOf(httpMethod));
        Mockito.when(mockRequest.getUri()).thenReturn(URI.create("/changePassword"));

        // Set up headers
        HashMap<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
        Mockito.when(mockRequest.getHeaders()).thenReturn(headers);

        // Mock the createResponseBuilder to return the mocked builder
        Mockito.when(mockRequest.createResponseBuilder(HttpStatus.OK)).thenReturn(mockBuilder);

        // Set up the builder methods to return the builder itself for chaining
        Mockito.when(mockBuilder.body(Mockito.any())).thenReturn(mockBuilder);
        Mockito.when(mockBuilder.header(Mockito.anyString(), Mockito.anyString())).thenReturn(mockBuilder);
        Mockito.when(mockBuilder.build()).thenReturn(mockResponse);

        return new HttpRequestTranslator(mockRequest);
    }

}
