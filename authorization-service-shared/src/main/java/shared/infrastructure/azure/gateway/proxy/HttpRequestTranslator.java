package shared.infrastructure.azure.gateway.proxy;

import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HttpRequestTranslator {

    private final HttpRequestMessage<Optional<String>> request;
    private final String body;
    private final Map<String, String> headers;

    public HttpRequestTranslator(HttpRequestMessage<Optional<String>> request) {
        this.request = request;
        this.body = request.getBody().orElse("");
        this.headers = new HashMap<>(request.getHeaders());
    }

    // Overloaded constructor to allow a modified body and headers
    private HttpRequestTranslator(HttpRequestMessage<Optional<String>> request, String body, Map<String, String> headers) {
        this.request = request;
        this.body = body;
        this.headers = headers;
    }

    public static HttpRequestTranslator ofNullable(HttpRequestMessage<Optional<String>> request) {
        return new HttpRequestTranslator(request);
    }

    public String getPath() {
        // Retrieve the path from the request URI
        return request.getUri().getPath();
    }

    public String getHttpMethod() {
        // Retrieve the HTTP method, e.g., GET, POST
        return request.getHttpMethod().name();
    }

    public Map<String, String> getHeaders() {
        return request.getHeaders();
    }

    public Map<String, String> getQueryStringParameters() {
        return request.getQueryParameters();
    }

    public String getBody() {
        return request.getBody().orElse("");
    }

    public String getHeader(String header) {
        return request.getHeaders().getOrDefault(header, "");
    }

    public HttpRequestTranslator withUpdatedBody(String newBody) {
        Map<String, String> updatedHeaders = new HashMap<>(headers);
        updatedHeaders.put("Content-Type", "application/json");
        return new HttpRequestTranslator(request, newBody, updatedHeaders);
    }

    public HttpResponseMessage.Builder createResponseBuilder(HttpStatus status) {
        return request.createResponseBuilder(status);
    }

    public HttpResponseMessage.Builder createResponseBuilder(HttpStatus status, String contentType, Object responseBody) {
        return request.createResponseBuilder(status)
                .header("Content-Type", contentType)
                .body(responseBody);
    }
    // Add additional methods if needed to retrieve path parameters, etc.
}
