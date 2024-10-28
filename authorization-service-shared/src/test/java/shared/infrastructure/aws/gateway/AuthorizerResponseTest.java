package shared.infrastructure.aws.gateway;


import com.amazonaws.util.json.Jackson;
import com.azure.resourcemanager.authorization.models.Permission;
import com.azure.resourcemanager.authorization.models.RoleDefinition;
import shared.TestHelper;
import org.testng.Assert;
import org.testng.annotations.Test;
import shared.infrastructure.azure.gateway.AuthorizerResponse;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AuthorizerResponseTest {
    @Test
    public void testBuild() {
        // Mock the Permission object
        Permission permission = Mockito.mock(Permission.class);
        Mockito.when(permission.actions()).thenReturn(Arrays.asList("Microsoft.Web/sites/*/invoke"));
        Mockito.when(permission.notActions()).thenReturn(Arrays.asList("Microsoft.Web/sites/restrictedAction"));

        // Mock the RoleDefinition object
        RoleDefinition roleDefinition = Mockito.mock(RoleDefinition.class);
        Mockito.when(roleDefinition.roleName()).thenReturn("customRole");
        Mockito.when(roleDefinition.permissions()).thenReturn(new HashSet<>( Collections.singletonList(permission)));
        Mockito.when(roleDefinition.assignableScopes()).thenReturn(
                new HashSet<>(Arrays.asList("/subscriptions/123456/resourceGroups/myResourceGroup"))); // Return a Set instead of List


                // Build an AuthorizerResponse using the mocked RoleDefinition
        AuthorizerResponse response = new AuthorizerResponse.Builder()
                .withPrincipalId("32944624-1f4a-4f34-bdf6-5450679ef1bf")
                .withRoleDefinition(roleDefinition) // Use mocked RoleDefinition
                .withContextAsString("stringKey", "value")
                .withContextAsNumber("numberKey", 1)
                .withContextAsNumber("doubleKey", 1.1)
                .withContextAsBoolean("booleanKey", true)
                .build();

        // Assertions to validate the response
        Assert.assertNotNull(response);
        Assert.assertEquals(
                Jackson.toJsonString(response),
                TestHelper.resourceAsJsonString("/ApiGatewayAuthorizerResponse.json"));
    }


}