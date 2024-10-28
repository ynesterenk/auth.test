package server.core.translator;

import authorization.server.core.translator.ClientCredentialsRequestTranslator;
import server.TestHelper;
import authorization.server.core.model.ClientCredentialsRequest;
import shared.infrastructure.azure.gateway.proxy.HttpRequestTranslator;
import com.microsoft.azure.functions.HttpRequestMessage;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ClientCredentialsRequestTranslatorTest {

    @Test
    public void testTranslateBody() {
        HttpRequestTranslator request = makeHttpRequestTranslator(
                TestHelper.resourceAsString("/ClientCredentialsRequest.json"), Collections.emptyMap());

        ClientCredentialsRequest actual = ClientCredentialsRequestTranslator.from(request);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual.getGrantType(), "client_credentials");
        Assert.assertEquals(actual.getClientId(), "1234567890");
        Assert.assertEquals(actual.getClientSecret(), "zaq1xsw2cde3vfr4bgt5nhy6");
    }

    @Test
    public void testTranslateHeader() {
        Map<String, String> headers = Collections.singletonMap(
                "Authorization", "Basic MTIzNDU2Nzg5MDp6YXExeHN3MmNkZTN2ZnI0Ymd0NW5oeTY=");
        HttpRequestTranslator request = makeHttpRequestTranslator(
                "{\"grant_type\":\"client_credentials\"}", headers);

        ClientCredentialsRequest actual = ClientCredentialsRequestTranslator.from(request);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual.getGrantType(), "client_credentials");
        Assert.assertEquals(actual.getClientId(), "1234567890");
        Assert.assertEquals(actual.getClientSecret(), "zaq1xsw2cde3vfr4bgt5nhy6");
    }

    @Test
    public void testEmpty() {
        HttpRequestTranslator request = makeHttpRequestTranslator("{}", Collections.emptyMap());

        ClientCredentialsRequest actual = ClientCredentialsRequestTranslator.from(request);

        Assert.assertNotNull(actual);
        Assert.assertNull(actual.getGrantType());
        Assert.assertNull(actual.getClientId());
        Assert.assertNull(actual.getClientSecret());
    }

    private static HttpRequestTranslator makeHttpRequestTranslator(String body, Map<String, String> headers) {
        // Mock HttpRequestMessage to create an HttpRequestTranslator
        HttpRequestMessage<Optional<String>> mockRequest = Mockito.mock(HttpRequestMessage.class);

        // Set headers and body for the request
        Mockito.when(mockRequest.getHeaders()).thenReturn(headers);
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of(body));

        return new HttpRequestTranslator(mockRequest);
    }
}
