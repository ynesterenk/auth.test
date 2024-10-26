package shared.infrastructure.azure.gateway.proxy;

import lombok.Data;
import lombok.Getter;

import java.util.Map;

@Data
public class ProxyRequest {

    private String resource;
    private String path;
    private String httpMethod;
    private Map<String, String> headers;
    private Map<String, String> queryStringParameters;
    private Map<String, String> pathParameters;
    private Map<String, String> stageVariables;
    private Map<String, ?> requestContext;
    private String body;
    private Boolean isBase64Encoded;

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body=body;
    }

}
