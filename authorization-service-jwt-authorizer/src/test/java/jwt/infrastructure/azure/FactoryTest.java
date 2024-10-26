package jwt.infrastructure.azure;
import authorization.jwt.core.Jwt;
import authorization.jwt.core.PolicyRepository;
import authorization.jwt.infrastructure.azure.Factory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class FactoryTest {

    private Factory factory;

    @BeforeMethod
    public void setUp() {
        Map<String, String> env = new HashMap<>();
        env.put("AZURE_AD_SUBSCRIPTION_ID", "1757ec62-3908-48fd-a5c9-2d320fb26e4f");
        env.put("AZURE_AD_TENANT_ID", "b580e134-0c17-4a0e-9fa1-4aff04ad87f6");
        factory = new Factory(env);
    }

    @Test
    public void testCreateJsonWebToken() {
        Jwt actual = factory.createJsonWebToken();

        Assert.assertNotNull(actual);
    }

    @Test
    public void testCreateRolePolicyRepository() {
        PolicyRepository actual = factory.createRolePolicyRepository();

        Assert.assertNotNull(actual);
    }

}