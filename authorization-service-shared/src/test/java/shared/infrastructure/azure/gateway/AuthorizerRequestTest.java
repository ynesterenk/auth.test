package shared.infrastructure.azure.gateway;


import shared.TestHelper;
import org.testng.Assert;
import org.testng.annotations.Test;
import shared.infrastructure.azure.gateway.AuthorizerRequest;
import shared.infrastructure.azure.gateway.JsonUtil;

public class AuthorizerRequestTest {

    @Test
    public void testFromJson() {
        AuthorizerRequest request = JsonUtil.fromJsonString(
            TestHelper.resourceAsString("/ApiGatewayAuthorizerRequest.json"),
            AuthorizerRequest.class);

        Assert.assertNotNull(request);
        Assert.assertEquals(request.getType(), "TOKEN");
        Assert.assertEquals(request.getAuthorizationToken(), "Basic dXNlcjoxMjM0");
        Assert.assertEquals(request.getMethodArn(), "arn:aws:execute-api:eu-west-1:1234567890:qwerty12345/v1/GET/resource/*");
    }

}