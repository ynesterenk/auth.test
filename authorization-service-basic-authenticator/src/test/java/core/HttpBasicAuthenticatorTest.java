package core;

import shared.core.http.BasicAuthenticationException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Collectors;

public class HttpBasicAuthenticatorTest {

    @Mock
    private UserPool mockUserPool;

    private HttpBasicAuthenticator basicAuthenticator;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        basicAuthenticator = new HttpBasicAuthenticator(mockUserPool);

        // Simulate Azure AD JWT response for a successful authentication
        String accessToken = new BufferedReader(new InputStreamReader(
                HttpBasicAuthenticatorTest.class.getResourceAsStream("/AzureAccessToken.jwt")))
                .lines().collect(Collectors.joining());

        // Configure mock to return an Azure AD access token for valid credentials
        Mockito.when(mockUserPool.verify(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(accessToken);
    }

    @Test
    public void testAuth() {
        // Test a valid Azure AD token
        Principal actual = basicAuthenticator.authenticate(
                "Basic aHR0cHdhdGNoOmY=");

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual.getId(), "1234567890"); // Replace with the expected Azure AD subject ID
        Assert.assertEquals(actual.getUsername(), "admin");
        Assert.assertEquals(actual.getScope(), Arrays.asList("User.Read", "Files.Read")); // Adjust to expected Azure AD scopes
        Assert.assertEquals(actual.getExpirationTime(), (Long) 1234567890000L); // Adjust to token's expiration time
        Mockito.verify(mockUserPool).verify("httpwatch", "f");
    }

    @DataProvider
    public Object[][] samples() {
        return new Object[][]{
                {null}, {""}, {"Basic aHR0cHdhdGNoOmY"}, {"Basic"}, {"aHR0cHdhdGNoOmY="},
                {"Base aHR0cHdhdGNoOmY="}, {"Basic YWFhOmJiYjpjY2M="}, {"Basic Og=="},
                {"Basic YTo="}, {"Basic OmE="}
        };
    }

    @Test(dataProvider = "samples", expectedExceptions = BasicAuthenticationException.class)
    public void testFailAuth(String header) {
        // Test invalid or malformed tokens
        Principal actual = basicAuthenticator.authenticate(header);

        Assert.assertNull(actual);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testFailAuthWithException() {
        // Reset and simulate an error from Azure AD during token verification
        Mockito.reset(mockUserPool);
        Mockito.when(mockUserPool.verify("httpwatch", "f"))
                .thenThrow(RuntimeException.class);

        Principal actual = basicAuthenticator.authenticate("Basic aHR0cHdhdGNoOmY=");

        Assert.assertNull(actual);
    }
}
