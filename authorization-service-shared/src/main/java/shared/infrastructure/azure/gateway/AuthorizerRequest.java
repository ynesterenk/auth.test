package shared.infrastructure.azure.gateway;

import lombok.Data;

@Data
public class AuthorizerRequest {

    private String type;
    private String authorizationToken;
    private String methodArn;
}