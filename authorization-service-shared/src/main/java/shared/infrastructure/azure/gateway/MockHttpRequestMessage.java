package shared.infrastructure.azure.gateway;

import com.microsoft.azure.functions.*;

import java.net.URI;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

// Minimal implementation of HttpRequestMessage for real request handling
public class MockHttpRequestMessage implements HttpRequestMessage<Optional<AuthorizerRequest>> {
    private final AuthorizerRequest request;

    public MockHttpRequestMessage(AuthorizerRequest request) {
        this.request = request;
    }

    @Override
    public URI getUri() {
        return URI.create("https://your-function-url"); // Replace with actual URL
    }

    @Override
    public HttpMethod getHttpMethod() {
        return HttpMethod.POST;
    }

    @Override
    public Optional<AuthorizerRequest> getBody() {
        return Optional.of(request);
    }

    @Override
    public Map<String, String> getHeaders() {
        return Collections.singletonMap("Authorization", GetBearerToken());
    }

    @Override
    public Map<String, String> getQueryParameters() {
        return Map.of();
    }

    @Override
    public HttpResponseMessage.Builder createResponseBuilder(HttpStatus status) {
        return new DefaultHttpResponseMessageBuilder(status);
    }

    @Override
    public HttpResponseMessage.Builder createResponseBuilder(HttpStatusType httpStatusType) {
        return null;
    }

    // Implement other methods if necessary
    public static String GetBearerToken() {
        String username = "admin";
        String password = "password123";
        String credentials = username + ":" + password;

        // Encode credentials to Base64
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        // Format as Basic Auth header
        return "Basic " + encodedCredentials;
    }
}