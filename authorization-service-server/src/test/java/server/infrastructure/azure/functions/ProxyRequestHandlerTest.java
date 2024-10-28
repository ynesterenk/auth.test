
package server.infrastructure.azure.functions;

import authorization.server.infrastructure.azure.functions.ProxyRequestHandler;
import com.microsoft.azure.functions.*;
import authorization.server.core.facade.ClientCredentialsFacade;
import authorization.server.infrastructure.azure.Factory;
import shared.core.validation.ErrorState;
import shared.core.validation.ValidationException;
import shared.infrastructure.azure.gateway.proxy.HttpRequestTranslator;
import shared.infrastructure.azure.gateway.proxy.HttpError;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static server.TestHelper.SetupMockResponse;

public class ProxyRequestHandlerTest {

    @Mock
    private Factory mockFactory;
    @Mock
    private ClientCredentialsFacade mockClientCredentialsFacade;
    @Mock
    private ExecutionContext mockContext;

    private ProxyRequestHandler handler;
    private HttpRequestMessage<Optional<String>> mockRequest;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(mockFactory.createClientCredentialsFacade())
                .thenReturn(mockClientCredentialsFacade);
        handler = new ProxyRequestHandler(mockFactory);

        mockRequest = Mockito.mock(HttpRequestMessage.class);
    }

    @Test
    public void testNotFound() {
        Mockito.when(mockRequest.getUri()).thenReturn(URI.create("/foo"));
        Mockito.when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of("foo"));
        SetupMockResponse(mockRequest, HttpStatus.NOT_FOUND);
        HttpResponseMessage actual = handler.handleRequest(mockRequest, mockContext);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual.getStatusCode(), HttpStatus.NOT_FOUND.value());
        HttpError error = (HttpError) actual.getBody();
        Assert.assertEquals(error.getMessage(), "Not Found");
    }

    @Test
    public void testBadRequest() throws Exception {
        Mockito.when(mockRequest.getUri()).thenReturn(URI.create("/oauth/token"));
        Mockito.when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        Mockito.when(mockRequest.getHeaders()).thenReturn(Collections.emptyMap());
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of("foo"));

        ErrorState errorState = new ErrorState();
        errorState.addError("key", "error message");
        Mockito.when(mockClientCredentialsFacade.process(Mockito.any()))
                .thenThrow(new ValidationException(errorState));
        SetupMockResponse(mockRequest, HttpStatus.BAD_REQUEST);
        HttpResponseMessage actual = handler.handleRequest(mockRequest, mockContext);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual.getStatusCode(), HttpStatus.BAD_REQUEST.value());
        HttpError error = (HttpError) actual.getBody();
        Assert.assertEquals(error.getMessage(), "Validation error");
        Assert.assertTrue(error.getErrors().containsKey("key"));
        Assert.assertEquals(error.getErrors().get("key"), "error message");
    }

    @Test(expectedExceptions = Exception.class)
    public void testInternalServerError() throws Exception {
        Mockito.when(mockRequest.getUri()).thenReturn(URI.create("/oauth/token"));
        Mockito.when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of("foo"));
        Mockito.when(mockClientCredentialsFacade.process(Mockito.any()))
                .thenThrow(Exception.class);
        SetupMockResponse(mockRequest, HttpStatus.OK);

        handler.handleRequest(mockRequest, mockContext);
    }

    @Test
    public void testInvokeOAuth2ClientCredentialsFacade() throws Exception {
        Mockito.when(mockRequest.getUri()).thenReturn(URI.create("/oauth/token"));
        Mockito.when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of("foo"));
        HttpResponseMessage response = Mockito.mock(HttpResponseMessage.class);
        Mockito.when(response.getStatusCode()).thenReturn(HttpStatus.OK.value());
        Mockito.when(response.getBody()).thenReturn("OK");

        Mockito.when(mockClientCredentialsFacade.process(Mockito.any()))
                .thenReturn(response);
        SetupMockResponse(mockRequest, HttpStatus.OK);
        HttpResponseMessage actual = handler.handleRequest(mockRequest, mockContext);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual.getStatusCode(), HttpStatus.OK.value());
        Assert.assertEquals(actual.getBody(), "OK");
        Mockito.verify(mockClientCredentialsFacade).process(Mockito.any());
    }
}
