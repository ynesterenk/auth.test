package authorization.server.infrastructure.azure;

import authorization.server.core.UserPool;
import authorization.server.core.ValidationRules;
import authorization.server.core.facade.ChangePasswordFacade;
import authorization.server.core.facade.ClientCredentialsFacade;
import authorization.server.infrastructure.azure.ad.AzureADUserPool;
import shared.core.validation.ErrorState;
import lombok.Getter;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.util.Arrays;
import java.util.Map;

public class Factory {

    private static final String AZURE_AD_TENANT_ID = "AZURE_AD_TENANT_ID";
    private static final String AZURE_AD_CLIENT_ID = "AZURE_AD_CLIENT_ID";
    private static final String AZURE_AD_CLIENT_SECRET = "AZURE_AD_CLIENT_SECRET";
    private static final String AZURE_AD_AUTHORITY = "https://login.microsoftonline.com/%s";

    @Getter(lazy = true)
    private static final Factory instance = new Factory(System.getenv());

    private final UserPool userPool;
    private final VelocityEngine velocityEngine;

    public Factory(Map<String, String> env) {
        String tenantId = env.get(AZURE_AD_TENANT_ID);
        String clientId = env.get(AZURE_AD_CLIENT_ID);
        String clientSecret = env.get(AZURE_AD_CLIENT_SECRET);

        userPool = createAzureADUserPool(clientId, clientSecret, tenantId);
        velocityEngine = createVelocityEngine();
    }

    public ClientCredentialsFacade createClientCredentialsFacade() {
        return new ClientCredentialsFacade(
                new ErrorState(),
                userPool,
                Arrays.asList(
                        ValidationRules::verifyBody,
                        ValidationRules::verifyBasicAuthenticationHeader),
                Arrays.asList(
                        ValidationRules::verifyGrantType,
                        ValidationRules::verifyClientId,
                        ValidationRules::verifyClientSecret));
    }

    public ChangePasswordFacade createChangePasswordFacade() {
        return new ChangePasswordFacade(
                new ErrorState(),
                userPool,
                velocityEngine.getTemplate("public/index.html"),
                Arrays.asList(
                        ValidationRules::verifyBody,
                        ValidationRules::verifyBasicAuthenticationHeader),
                Arrays.asList(
                        ValidationRules::verifyUserName,
                        ValidationRules::verifyPreviousPassword,
                        ValidationRules::verifyProposedPassword));
    }

    private UserPool createAzureADUserPool(String clientId, String clientSecret, String tenantId) {
        String authority = String.format(AZURE_AD_AUTHORITY, tenantId);
        return new AzureADUserPool(clientId, clientSecret, authority);
    }

    private static VelocityEngine createVelocityEngine() {
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.SimpleLog4JLogSystem");
        ve.setProperty("runtime.log.logsystem.log4j.category", "velocity");
        ve.setProperty("runtime.log.logsystem.log4j.logger", "velocity");
        ve.setProperty("resource.loader", "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();
        return ve;
    }
}
