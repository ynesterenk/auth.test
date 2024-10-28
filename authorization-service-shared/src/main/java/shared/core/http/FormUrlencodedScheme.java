package shared.core.http;

import com.amazonaws.util.json.Jackson;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.infrastructure.azure.gateway.proxy.HttpRequestTranslator;
import shared.infrastructure.azure.gateway.proxy.ProxyRequest;
import lombok.SneakyThrows;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;

import java.util.Map;
import java.util.stream.Collectors;

public class FormUrlencodedScheme {

    private FormUrlencodedScheme() {
        super();
    }

    public static HttpRequestTranslator decode(HttpRequestTranslator request) {
        Map<String, String> headers = request.getHeaders();
        String contentType = headers.get(HttpHeaders.CONTENT_TYPE);

        // Check if content type is application/x-www-form-urlencoded
        if (contentType == null || !contentType.contains("application/x-www-form-urlencoded")) {
            return request;
        }

        // Decode the form-urlencoded body to JSON
        String decodedBody;
        try {
            Map<String, String> decodedParams = decode(request.getBody(), headers);
            decodedBody = new ObjectMapper().writeValueAsString(decodedParams);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode form-urlencoded body", e);
        }

        // Create a new HttpRequestTranslator with the JSON body and updated content type
        HttpRequestTranslator modifiedRequest = request.withUpdatedBody(decodedBody);
        modifiedRequest.getHeaders().put(HttpHeaders.CONTENT_TYPE, "application/json");
        return modifiedRequest;
    }

    public static ProxyRequest decode(ProxyRequest request) {
        Map<String, String> headers = request.getHeaders();
        String contentType = headers.get(HttpHeaders.CONTENT_TYPE);
        if (contentType == null || !contentType.contains(URLEncodedUtils.CONTENT_TYPE)) {
            return request;
        }

        request.setBody(Jackson.toJsonString(
            decode(request.getBody(), headers)));
        headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
        return request;
    }


    @SneakyThrows
    public static Map<String, String> decode(String body, Map<String, String> headers) {
        StringEntity entity = new StringEntity(body);
        entity.setContentType(headers.get(HttpHeaders.CONTENT_TYPE));

        return URLEncodedUtils.parse(entity).stream()
            .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
    }

}
