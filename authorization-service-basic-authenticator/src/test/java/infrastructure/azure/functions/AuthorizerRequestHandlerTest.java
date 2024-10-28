package com.github.vitalibo.authorization.basic.infrastructure.azure.functions;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.github.vitalibo.authorization.basic.core.HttpBasicAuthenticator;
import com.github.vitalibo.authorization.basic.core.Principal;
import com.github.vitalibo.authorization.basic.infrastructure.azure.Factory;
import com.github.vitalibo.authorization.shared.core.http.BasicAuthenticationException;
import com.github.vitalibo.authorization.shared.infrastructure.azure.gateway.AuthorizerRequest;
import com.github.vitalibo.authorization.shared.infrastructure.azure.gateway.AuthorizerResponse;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

public class AuthorizerRequestHandlerTest {

    @Mock
    private Factory mockFactory;
    @Mock
    private HttpBasicAuthenticator mockHttpBasicAuthenticator;
    @Mock
    private HttpRequestMessage<AuthorizerRequest> mockRequest;
    @Mock
    private ExecutionContext mockContext;

    private AuthorizerRequestHandler handler;
    private AuthorizerRequest request;
    private Principal principal;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(mockFactory.createHttpBasicAuthenticator()).thenReturn(mockHttpBasicAuthenticator);
        handler = new AuthorizerRequestHandler(mockFactory);
        request = makeRequest();
        principal = makePrincipal();
    }

    @Test
    public void testAuthentication() {
        Mockito.when(mockHttpBasicAuthenticator.authenticate(Mockito.any()))
            .thenReturn(principal);
        Mockito.when(mockRequest.getBody()).thenReturn(request);

        HttpResponseMessage response = handler.handleRequest(mockRequest, mockContext);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatus(), HttpStatus.OK);
        AuthorizerResponse actual = (AuthorizerResponse) response.getBody();
        Assert.assertEquals(actual.getPrincipalId(), principal.getId());
        Assert.assertTrue(actual.getPolicyDocument().toJson().contains("Allow"));
        Assert.assertEquals(actual.getContext().get("username"), principal.getUsername());
        Assert.assertEquals(actual.getContext().get("scope"), "foo,bar");
        Assert.assertEquals(actual.getContext().get("expirationTime"), 1234567890L);
    }

    @Test
    public void testUnauthorized() {
        Mockito.when(mockHttpBasicAuthenticator.authenticate(Mockito.any()))
            .thenThrow(BasicAuthenticationException.class);
        Mockito.when(mockRequest.getBody()).thenReturn(request);

        HttpResponseMessage response = handler.handleRequest(mockRequest, mockContext);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatus(), HttpStatus.OK);
        AuthorizerResponse actual = (AuthorizerResponse) response.getBody();
        Assert.assertEquals(actual.getPrincipalId(), null);
        Assert.assertTrue(actual.getPolicyDocument().toJson().contains("Deny"));
        Assert.assertEquals(actual.getContext().get("username"), null);
        Assert.assertEquals(actual.getContext().get("scope"), null);
        Assert.assertEquals(actual.getContext().get("expirationTime"), null);
    }

    @Test
    public void testValidationError() {
        Mockito.when(mockHttpBasicAuthenticator.authenticate(Mockito.any()))
            .thenThrow(BasicAuthenticationException.class);
        Mockito.when(mockRequest.getBody()).thenReturn(request);

        HttpResponseMessage response = handler.handleRequest(mockRequest, mockContext);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatus(), HttpStatus.OK);
        AuthorizerResponse actual = (AuthorizerResponse) response.getBody();
        Assert.assertEquals(actual.getPrincipalId(), null);
        Assert.assertTrue(actual.getPolicyDocument().toJson().contains("Deny"));
        Assert.assertEquals(actual.getContext().get("username"), null);
        Assert.assertEquals(actual.getContext().get("scope"), null);
        Assert.assertEquals(actual.getContext().get("expirationTime"), null);
    }

    @Test(expectedExceptions = Exception.class)
    public void testInternalServerError() {
        Mockito.when(mockHttpBasicAuthenticator.authenticate(Mockito.any()))
            .thenThrow(RuntimeException.class);
        Mockito.when(mockRequest.getBody()).thenReturn(request);

        handler.handleRequest(mockRequest, mockContext);
    }

    private static AuthorizerRequest makeRequest() {
        AuthorizerRequest request = new AuthorizerRequest();
        request.setMethodArn("arn:aws:::");
        request.setAuthorizationToken("Basic dXNlcjpwYXNzd29yZA==");
        return request;
    }

    private static Principal makePrincipal() {
        Principal principal = new Principal();
        principal.setId("32944624-1f4a-4f34-bdf6-5450679ef1bf");
        principal.setUsername("admin");
        principal.setScope(Arrays.asList("foo", "bar"));
        principal.setExpirationTime(1234567890L);
        return principal;
    }

}