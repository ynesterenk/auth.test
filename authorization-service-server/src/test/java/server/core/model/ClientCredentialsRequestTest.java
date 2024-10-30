package server.core.model;

import authorization.server.core.model.ClientCredentialsRequest;
import server.TestHelper;
import org.testng.Assert;
import org.testng.annotations.Test;
import shared.infrastructure.azure.gateway.JsonUtil;

public class ClientCredentialsRequestTest {

    @Test
    public void testFromJson() {
        ClientCredentialsRequest request = JsonUtil.fromJsonString(
            TestHelper.resourceAsString("/ClientCredentialsRequest.json"),
            ClientCredentialsRequest.class);

        Assert.assertNotNull(request);
        Assert.assertEquals(request.getGrantType(), "client_credentials");
        Assert.assertEquals(request.getClientId(), "1234567890");
        Assert.assertEquals(request.getClientSecret(), "zaq1xsw2cde3vfr4bgt5nhy6");
    }

}