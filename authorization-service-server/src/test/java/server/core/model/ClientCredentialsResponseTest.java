package server.core.model;

import authorization.server.core.model.ClientCredentialsResponse;
import server.TestHelper;
import org.testng.Assert;
import org.testng.annotations.Test;
import shared.infrastructure.azure.gateway.JsonUtil;

public class ClientCredentialsResponseTest {

    @Test
    public void testToJson() {
        ClientCredentialsResponse response = new ClientCredentialsResponse();
        response.setAccessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
        response.setExpiresIn(1234567890L);
        response.setTokenType("Bearer");

        String actual = JsonUtil.toJsonString(response);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual, TestHelper.resourceAsJsonString("/ClientCredentialsResponse.json"));
    }

}