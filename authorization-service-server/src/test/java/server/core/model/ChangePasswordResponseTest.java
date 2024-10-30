package server.core.model;

import authorization.server.core.model.ChangePasswordResponse;
import server.TestHelper;
import org.testng.Assert;
import org.testng.annotations.Test;
import shared.infrastructure.azure.gateway.JsonUtil;

public class ChangePasswordResponseTest {

    @Test
    public void testToJson() {
        ChangePasswordResponse response = new ChangePasswordResponse();
        response.setAcknowledged(true);
        response.setMessage("foo bar");

        String actual = JsonUtil.toJsonString(response);

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual, TestHelper.resourceAsJsonString("/ChangePasswordResponse.json"));
    }

}