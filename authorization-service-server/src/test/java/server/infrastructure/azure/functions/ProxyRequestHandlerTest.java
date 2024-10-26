
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

public class ProxyRequestHandlerTest {

    @Mock
    private Factory mockFactory;
    @Mock
    private ClientCredentialsFacade mockClientCredentialsFacade;
    @Mock
    private ExecutionContext mockContext;

    private ProxyRequestHandler lambda;
    private HttpRequestMessage<Optional<String>> mockRequest;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(mockFactory.createClientCredentialsFacade())
                .thenReturn(mockClientCredentialsFacade);
        lambda = new ProxyRequestHandler(mockFactory);

        mockRequest = Mockito.mock(HttpRequestMessage.class);
    }

    @Test
    public void testNotFound() {
        Mockito.when(mockRequest.getUri()).thenReturn(URI.create("/foo"));
        Mockito.when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.GET);

        HttpResponseMessage actual = lambda.handleRequest(mockRequest, mockContext);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual.getStatusCode(), HttpStatus.NOT_FOUND);
        HttpError error = (HttpError) actual.getBody();
        Assert.assertEquals(error.getMessage(), "Not Found");
        Assert.assertEquals(error.getRequestId(), mockContext.getInvocationId());
    }

    @Test
    public void testBadRequest() throws Exception {
        Mockito.when(mockRequest.getUri()).thenReturn(URI.create("/oauth/token"));
        Mockito.when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        Mockito.when(mockRequest.getHeaders()).thenReturn(Collections.emptyMap());

        ErrorState errorState = new ErrorState();
        errorState.addError("key", "error message");
        Mockito.when(mockClientCredentialsFacade.process(Mockito.any()))
                .thenThrow(new ValidationException(errorState));

        HttpResponseMessage actual = lambda.handleRequest(mockRequest, mockContext);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual.getStatusCode(), HttpStatus.BAD_REQUEST);
        HttpError error = (HttpError) actual.getBody();
        Assert.assertEquals(error.getMessage(), "Validation error");
        Assert.assertTrue(error.getErrors().containsKey("key"));
        Assert.assertEquals(error.getErrors().get("key"), "error message");
    }

    @Test(expectedExceptions = Exception.class)
    public void testInternalServerError() throws Exception {
        Mockito.when(mockRequest.getUri()).thenReturn(URI.create("/oauth/token"));
        Mockito.when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);
        Mockito.when(mockClientCredentialsFacade.process(Mockito.any()))
                .thenThrow(Exception.class);

        lambda.handleRequest(mockRequest, mockContext);
    }

    @Test
    public void testInvokeOAuth2ClientCredentialsFacade() throws Exception {
        Mockito.when(mockRequest.getUri()).thenReturn(URI.create("/oauth/token"));
        Mockito.when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.POST);

        HttpResponseMessage response = Mockito.mock(HttpResponseMessage.class);
        Mockito.when(response.getStatusCode()).thenReturn(HttpStatus.OK.value());
        Mockito.when(response.getBody()).thenReturn("OK");

        Mockito.when(mockClientCredentialsFacade.process(Mockito.any()))
                .thenReturn(response);

        HttpResponseMessage actual = lambda.handleRequest(mockRequest, mockContext);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual.getStatusCode(), HttpStatus.OK);
        Assert.assertEquals(actual.getBody(), "OK");
        Mockito.verify(mockClientCredentialsFacade).process(Mockito.any());
    }
}
