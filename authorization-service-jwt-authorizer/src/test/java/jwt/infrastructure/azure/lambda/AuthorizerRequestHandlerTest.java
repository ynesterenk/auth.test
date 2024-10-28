package jwt.infrastructure.azure.lambda;

import authorization.jwt.core.Claims;
import authorization.jwt.core.Jwt;
import authorization.jwt.core.JwtVerificationException;
import authorization.jwt.core.PolicyRepository;
import authorization.jwt.infrastructure.azure.Factory;
import shared.infrastructure.azure.gateway.AuthorizerRequest;
import shared.infrastructure.azure.gateway.AuthorizerResponse;
import authorization.jwt.infrastructure.azure.functions.AuthorizerRequestHandler;
import com.azure.resourcemanager.authorization.models.Permission;
import com.azure.resourcemanager.authorization.models.RoleDefinition;
import com.microsoft.azure.functions.ExecutionContext;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class AuthorizerRequestHandlerTest {

    @Mock
    private Factory mockFactory;
    @Mock
    private Jwt mockJwt;
    @Mock
    private PolicyRepository mockPolicyRepository;

    @Mock
    private ExecutionContext mockContext;

    private AuthorizerRequestHandler handler;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(mockFactory.createJsonWebToken()).thenReturn(mockJwt);
        Mockito.when(mockFactory.createRolePolicyRepository()).thenReturn(mockPolicyRepository);
        handler = new AuthorizerRequestHandler(mockFactory);
    }

    @Test
    public void testUnauthorizedRequest() {
        Mockito.when(mockJwt.verify(Mockito.anyString())).thenThrow(JwtVerificationException.class);
        AuthorizerRequest request = makeAuthorizerRequest();

        AuthorizerResponse response = handler.handleRequest(request, mockContext);

        Assert.assertNull(response);
        Mockito.verify(mockJwt).verify(request.getAuthorizationToken());
        Mockito.verify(mockPolicyRepository, Mockito.never()).getRoleDefinitions(Mockito.any());
    }

    @Test
    public void testHandleRequest() {
        AuthorizerRequest request = makeAuthorizerRequest();
        Claims claims = makeClaims();
        Mockito.when(mockJwt.verify(Mockito.anyString())).thenReturn(claims);
        RoleDefinition roleDefinition = makeRoleDefinition();
        Mockito.when(mockPolicyRepository.getRoleDefinitions(claims))
                .thenReturn(Collections.singletonList(roleDefinition));
        //Mockito.when(mockRequest.getBody()).thenReturn(request);

        AuthorizerResponse response = handler.handleRequest(request, mockContext);



        Assert.assertNotNull(response);
        Assert.assertEquals(response.getPrincipalId(), claims.getUsername());
        Assert.assertNotNull(response.getPolicyDocument());
        Assert.assertTrue(response.getContext().isEmpty());
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testInternalServerError() {
        AuthorizerRequest request = makeAuthorizerRequest();
        Mockito.when(mockJwt.verify(Mockito.anyString())).thenReturn(makeClaims());
        Mockito.when(mockPolicyRepository.getRoleDefinitions(Mockito.any())).thenThrow(RuntimeException.class);


        handler.handleRequest(request, mockContext);
    }

    private static AuthorizerRequest makeAuthorizerRequest() {
        AuthorizerRequest request = new AuthorizerRequest();
        request.setType("token");
        request.setMethodArn("api-gateway-arn");
        request.setAuthorizationToken("jwt");
        return request;
    }

    private static Claims makeClaims() {
        Claims claims = new Claims();
        claims.setRoles(Collections.singletonList("roleId"));
        claims.setUsername("foo");
        return claims;
    }

    // Helper method to mock RoleDefinition and Permissions
    private static RoleDefinition makeRoleDefinition() {
        RoleDefinition roleDefinition = Mockito.mock(RoleDefinition.class);
        Permission permission = Mockito.mock(Permission.class);

        Mockito.when(permission.actions()).thenReturn(Arrays.asList("Microsoft.Web/sites/*/invoke"));
        Mockito.when(permission.notActions()).thenReturn(Arrays.asList("Microsoft.Web/sites/restrictedAction"));

        Mockito.when(roleDefinition.roleName()).thenReturn("customRole");
        Mockito.when(roleDefinition.permissions()).thenReturn(new HashSet<>(Collections.singletonList(permission)));
        Mockito.when(roleDefinition.assignableScopes()).thenReturn(
                new HashSet<>(Arrays.asList("/subscriptions/123456/resourceGroups/myResourceGroup"))
        );

        return roleDefinition;
    }
}
