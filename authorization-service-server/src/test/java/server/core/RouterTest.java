package server.core;

import authorization.server.core.Route;
import authorization.server.core.Router;
import com.microsoft.azure.functions.HttpMethod;
import shared.infrastructure.azure.gateway.proxy.HttpRequestTranslator;
import com.microsoft.azure.functions.HttpRequestMessage;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.net.URI;

import java.util.Optional;

public class RouterTest {

    @DataProvider
    public Object[][] samplesNotFound() {
        return new Object[][]{
                {sample("/", "GET")},
                {sample("/oauth/token", "GET")},
                {sample("/oauth/tokens", "POST")},
                {sample("/account", "PUT")}
        };
    }

    @Test(dataProvider = "samplesNotFound")
    public void testMatchNotFound(HttpRequestTranslator request) {
        Route actual = Router.match(request);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual, Route.NOT_FOUND);
    }

    @Test
    public void testMatchOAuth2Facade() {
        HttpRequestTranslator request = sample("/oauth/token", "POST");

        Route actual = Router.match(request);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual, Route.OAUTH2_CLIENT_CREDENTIALS);
    }

    @DataProvider
    public Object[][] samplesHttpMethod() {
        return new Object[][]{
                {"GET"}, {"POST"}
        };
    }

    @Test(dataProvider = "samplesHttpMethod")
    public void testMatchChangePassword(String httpMethod) {
        HttpRequestTranslator request = sample("/account", httpMethod);

        Route actual = Router.match(request);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual, Route.CHANGE_PASSWORD);
    }

    private static HttpRequestTranslator sample(String path, String httpMethod) {
        // Mock HttpRequestMessage to create an HttpRequestTranslator
        HttpRequestMessage<Optional<String>> mockRequest = Mockito.mock(HttpRequestMessage.class);

        // Set path and method for the request
        Mockito.when(mockRequest.getUri()).thenReturn(URI.create(path));
        Mockito.when(mockRequest.getHttpMethod()).thenReturn(HttpMethod.valueOf(httpMethod));
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of(""));
        return new HttpRequestTranslator(mockRequest);
    }
}
