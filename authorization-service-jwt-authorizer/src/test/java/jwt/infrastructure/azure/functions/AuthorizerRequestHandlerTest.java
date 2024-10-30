package jwt.infrastructure.azure.functions;

import authorization.jwt.core.Claims;
import authorization.jwt.core.Jwt;
import authorization.jwt.core.JwtVerificationException;
import authorization.jwt.core.PolicyRepository;
import authorization.jwt.infrastructure.azure.Factory;
import shared.infrastructure.azure.gateway.AuthorizerRequest;
import authorization.jwt.infrastructure.azure.functions.AuthorizerRequestHandler;
import com.azure.resourcemanager.authorization.models.Permission;
import com.azure.resourcemanager.authorization.models.RoleDefinition;
import com.microsoft.azure.functions.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import shared.infrastructure.azure.gateway.MockHttpRequestMessage;

import java.util.*;

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

        HttpRequestMessage<Optional<AuthorizerRequest>> httpRequest = new MockHttpRequestMessage(request);

        HttpResponseMessage response = handler.handleRequest(httpRequest, mockContext);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatus(), HttpStatus.UNAUTHORIZED);
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

        HttpRequestMessage<Optional<AuthorizerRequest>> httpRequest = new MockHttpRequestMessage(request);

        HttpResponseMessage response = handler.handleRequest(httpRequest, mockContext);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatus(), HttpStatus.OK);

        // Verify response content if needed (deserialize JSON if complex)
    }

    @Test
    public void testInternalServerError() {
        AuthorizerRequest request = makeAuthorizerRequest();
        Mockito.when(mockJwt.verify(Mockito.anyString())).thenReturn(makeClaims());
        Mockito.when(mockPolicyRepository.getRoleDefinitions(Mockito.any())).thenThrow(RuntimeException.class);

        HttpRequestMessage<Optional<AuthorizerRequest>> httpRequest = new MockHttpRequestMessage(request);

        HttpResponseMessage responseMessage=handler.handleRequest(httpRequest, mockContext);
        Assert.assertNotNull(responseMessage);
        Assert.assertEquals(responseMessage.getStatus(), HttpStatus.INTERNAL_SERVER_ERROR);
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