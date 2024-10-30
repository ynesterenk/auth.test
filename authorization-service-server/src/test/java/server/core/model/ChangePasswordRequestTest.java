package server.core.model;

import authorization.server.core.model.ChangePasswordRequest;
import server.TestHelper;
import org.testng.Assert;
import org.testng.annotations.Test;
import shared.infrastructure.azure.gateway.JsonUtil;

public class ChangePasswordRequestTest {

    @Test
    public void testFromJson() {
        ChangePasswordRequest request = JsonUtil.fromJsonString(
            TestHelper.resourceAsString("/ChangePasswordRequest.json"),
            ChangePasswordRequest.class);

        Assert.assertNotNull(request);
        Assert.assertEquals(request.getUsername(), "admin");
        Assert.assertEquals(request.getPreviousPassword(), "welcome");
        Assert.assertEquals(request.getProposedPassword(), "s3cr3t");
    }

}