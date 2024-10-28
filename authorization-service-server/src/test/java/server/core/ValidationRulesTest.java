package server.core;

import authorization.server.core.ValidationRules;
import authorization.server.core.model.ChangePasswordRequest;
import authorization.server.core.model.ClientCredentialsRequest;
import com.microsoft.azure.functions.HttpRequestMessage;
import shared.core.validation.ErrorState;
import shared.infrastructure.azure.gateway.proxy.HttpRequestTranslator;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import server.TestHelper;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static server.TestHelper.makeHttpRequestTranslator;
import static server.TestHelper.makeHttpRequestTranslatorWithHeaders;


public class ValidationRulesTest {

    private ErrorState errorState;

    @BeforeMethod
    public void setUp() {
        errorState = new ErrorState();
    }

    @Test
    public void testPassVerifyBody() {
        HttpRequestTranslator request = makeHttpRequestTranslator("{}");

        ValidationRules.verifyBody(request, errorState);

        Assert.assertFalse(errorState.hasErrors());
    }

    @DataProvider
    public Object[][] samples() {
        return new Object[][]{{null}, {""}};
    }

    @Test(dataProvider = "samples")
    public void testFailVerifyBody(String body) {
        HttpRequestTranslator request = makeHttpRequestTranslator(body);

        ValidationRules.verifyBody(request, errorState);

        Assert.assertTrue(errorState.hasErrors());
        Assert.assertNotNull(errorState.get("body"));
    }

    @DataProvider
    public Object[][] samplesBasicAuthenticationHeader() {
        return new Object[][]{
                {null}, {""}, {"Basic aHR0cHdhdGNoOmY="}
        };
    }

    @Test(dataProvider = "samplesBasicAuthenticationHeader")
    public void testPassVerifyBasicAuthenticationHeader(String header) {
        Map<String, String> headers = Collections.singletonMap("Authorization", header);
        HttpRequestTranslator request = makeHttpRequestTranslatorWithHeaders("{}", headers);

        ValidationRules.verifyBasicAuthenticationHeader(request, errorState);

        Assert.assertFalse(errorState.hasErrors());
    }

    @Test
    public void testFailVerifyBasicAuthenticationHeader() {
        Map<String, String> headers = Collections.singletonMap("Authorization", "incorrect value");
        HttpRequestTranslator request = makeHttpRequestTranslatorWithHeaders("{}", headers);

        ValidationRules.verifyBasicAuthenticationHeader(request, errorState);

        Assert.assertTrue(errorState.hasErrors());
        Assert.assertNotNull(errorState.get("Authorization"));
    }

    @Test
    public void testPassVerifyGrantType() {
        ClientCredentialsRequest request = new ClientCredentialsRequest();
        request.setGrantType("client_credentials");

        ValidationRules.verifyGrantType(request, errorState);

        Assert.assertFalse(errorState.hasErrors());
    }

    @DataProvider
    public Object[][] samplesIncorrectGrantType() {
        return new Object[][]{
                {null}, {""}, {"refresh_token"}, {"incorrect value"}
        };
    }

    @Test(dataProvider = "samplesIncorrectGrantType")
    public void testFailVerifyGrantType(String grantType) {
        ClientCredentialsRequest request = new ClientCredentialsRequest();
        request.setGrantType(grantType);

        ValidationRules.verifyGrantType(request, errorState);

        Assert.assertTrue(errorState.hasErrors());
        Assert.assertNotNull(errorState.get("grant_type"));
    }

    @Test
    public void testPassVerifyClientId() {
        ClientCredentialsRequest request = new ClientCredentialsRequest();
        request.setClientId("username");

        ValidationRules.verifyClientId(request, errorState);

        Assert.assertFalse(errorState.hasErrors());
    }

    @Test(dataProvider = "samples")
    public void testFailVerifyClientId(String clientId) {
        ClientCredentialsRequest request = new ClientCredentialsRequest();
        request.setClientId(clientId);

        ValidationRules.verifyClientId(request, errorState);

        Assert.assertTrue(errorState.hasErrors());
        Assert.assertNotNull(errorState.get("client_id"));
    }

    @Test
    public void testPassVerifyClientSecret() {
        ClientCredentialsRequest request = new ClientCredentialsRequest();
        request.setClientSecret("secret");

        ValidationRules.verifyClientSecret(request, errorState);

        Assert.assertFalse(errorState.hasErrors());
    }

    @Test(dataProvider = "samples")
    public void testFailVerifyClientSecret(String clientSecret) {
        ClientCredentialsRequest request = new ClientCredentialsRequest();
        request.setClientSecret(clientSecret);

        ValidationRules.verifyClientSecret(request, errorState);

        Assert.assertTrue(errorState.hasErrors());
        Assert.assertNotNull(errorState.get("client_secret"));
    }

    @Test
    public void testPassVerifyUserName() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setUsername("foo");

        ValidationRules.verifyUserName(request, errorState);

        Assert.assertFalse(errorState.hasErrors());
    }

    @Test(dataProvider = "samples")
    public void testFailVerifyUserName(String userName) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setUsername(userName);

        ValidationRules.verifyUserName(request, errorState);

        Assert.assertTrue(errorState.hasErrors());
        Assert.assertNotNull(errorState.get("username"));
    }

    @Test
    public void testPassVerifyPreviousPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setPreviousPassword("foo");

        ValidationRules.verifyPreviousPassword(request, errorState);

        Assert.assertFalse(errorState.hasErrors());
    }

    @Test(dataProvider = "samples")
    public void testFailVerifyPreviousPassword(String previousPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setPreviousPassword(previousPassword);

        ValidationRules.verifyPreviousPassword(request, errorState);

        Assert.assertTrue(errorState.hasErrors());
        Assert.assertNotNull(errorState.get("previous_password"));
    }

    @Test
    public void testPassVerifyProposedPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setProposedPassword("foo");

        ValidationRules.verifyProposedPassword(request, errorState);

        Assert.assertFalse(errorState.hasErrors());
    }

    @Test(dataProvider = "samples")
    public void testFailVerifyProposedPassword(String proposedPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setProposedPassword(proposedPassword);

        ValidationRules.verifyProposedPassword(request, errorState);

        Assert.assertTrue(errorState.hasErrors());
        Assert.assertNotNull(errorState.get("proposed_password"));
    }

    private static HttpRequestTranslator makeHttpRequestTranslator(String body) {
        HttpRequestMessage<Optional<String>> mockRequest = Mockito.mock(HttpRequestMessage.class);
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.ofNullable(body));
        return new HttpRequestTranslator(mockRequest);
    }

    private static HttpRequestTranslator makeHttpRequestTranslatorWithHeaders(String body, Map<String, String> headers) {
        HttpRequestMessage<Optional<String>> mockRequest = Mockito.mock(HttpRequestMessage.class);
        Mockito.when(mockRequest.getBody()).thenReturn(Optional.ofNullable(body));
        Mockito.when(mockRequest.getHeaders()).thenReturn(headers);
        return new HttpRequestTranslator(mockRequest);
    }
}
