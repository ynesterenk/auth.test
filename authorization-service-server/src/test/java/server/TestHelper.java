package server;

import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.mockito.Mockito;
import shared.infrastructure.azure.gateway.JsonUtil;
import shared.infrastructure.azure.gateway.proxy.HttpError;
import shared.infrastructure.azure.gateway.proxy.HttpHeaders;
import shared.infrastructure.azure.gateway.proxy.HttpRequestTranslator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;

public class TestHelper {

    private TestHelper() {
    }

    public static String resourceAsJsonString(String resource) {
        return JsonUtil.toJsonString(
            JsonUtil.fromJsonString(
                resourceAsString(resource), Object.class));
    }

    public static String resourceAsString(String resource) {
        return new BufferedReader(new InputStreamReader(TestHelper.class.getResourceAsStream(resource)))
            .lines().collect(Collectors.joining("\n"));
    }
    public static void SetupMockResponse(HttpRequestMessage<Optional<String>> mockRequest, HttpStatus httpStatus) {
        HttpResponseMessage.Builder mockBuilder = Mockito.mock(HttpResponseMessage.Builder.class);
        HttpResponseMessage mockResponse = Mockito.mock(HttpResponseMessage.class);
        Mockito.when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK.value());
        Mockito.when(mockResponse.getHeader(HttpHeaders.CONTENT_TYPE)).thenReturn("application/json");
        // Mock the createResponseBuilder to return the mocked builder
        Mockito.when(mockRequest.createResponseBuilder(httpStatus)).thenReturn(mockBuilder);
        // Set up the builder methods to return the builder itself for chaining
        Mockito.when(mockBuilder.body(Mockito.any())).thenReturn(mockBuilder);
        Mockito.when(mockBuilder.header(Mockito.anyString(), Mockito.anyString())).thenReturn(mockBuilder);
        Mockito.when(mockBuilder.build()).thenReturn(mockResponse);
        if(httpStatus == HttpStatus.UNAUTHORIZED) {
            Mockito.when(mockResponse.getBody()).thenReturn("authorization");
            Mockito.when(mockResponse.getStatusCode()).thenReturn(HttpStatus.UNAUTHORIZED.value());
        } else if (httpStatus == HttpStatus.NOT_FOUND) {
            HttpError error =new HttpError("Not Found", "id","state");
            Mockito.when(mockResponse.getBody()).thenReturn(error);
            Mockito.when(mockResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND.value());
        } else if (httpStatus == HttpStatus.BAD_REQUEST) {
            HttpError error =new HttpError("Validation error", "id","state");
            error.addError("key","error message");
            Mockito.when(mockResponse.getBody()).thenReturn(error);
            Mockito.when(mockResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST.value());
        }
        else
            Mockito.when(mockResponse.getBody()).thenReturn("jsonBody");
    }
    public static HttpRequestTranslator makeHttpRequestTranslatorWithHeaders(String body, Map<String, String> headers) {
        // Mock HttpRequestMessage
        HttpRequestMessage<Optional<String>> mockRequest = Mockito.mock(HttpRequestMessage.class);

        // Set body
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.ofNullable(body));

        // Set headers
        Mockito.when(mockRequest.getHeaders()).thenReturn(headers);

        // Wrap the mocked HttpRequestMessage in an HttpRequestTranslator
        return new HttpRequestTranslator(mockRequest);
    }
    public static HttpRequestTranslator makeHttpRequestTranslator(String body) {
        // Mock HttpRequestMessage to create an HttpRequestTranslator
        HttpRequestMessage<Optional<String>> mockRequest = Mockito.mock(HttpRequestMessage.class);

        // Set headers and body for the request
        HashMap<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
        Mockito.when(mockRequest.getHeaders()).thenReturn(headers);
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.of(body));

        return new HttpRequestTranslator(mockRequest);
    }

}
