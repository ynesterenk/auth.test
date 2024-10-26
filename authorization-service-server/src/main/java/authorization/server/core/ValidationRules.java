package authorization.server.core;

import authorization.server.core.model.ChangePasswordRequest;
import authorization.server.core.model.ClientCredentialsRequest;
import org.apache.commons.lang.StringUtils;
import shared.infrastructure.azure.gateway.proxy.HttpRequestTranslator;
import shared.core.http.BasicAuthenticationException;
import shared.core.http.BasicScheme;
import shared.core.validation.ErrorState;


public final class ValidationRules {

    private ValidationRules() {
    }

    public static void verifyBody(HttpRequestTranslator request, ErrorState errorState) {
        String body = request.getBody();

        if (StringUtils.isBlank(body)) {
            errorState.addError(
                    "body",
                    "Required not empty body");
        }
    }

    public static void verifyBasicAuthenticationHeader(HttpRequestTranslator request, ErrorState errorState) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.isBlank(authorization)) {
            return;
        }

        try {
            BasicScheme.decode(request.getHeaders());
        } catch (BasicAuthenticationException e) {
            errorState.addError(
                    "Authorization",
                    "Basic Authentication header has incorrect format");
        }
    }

    public static void verifyGrantType(ClientCredentialsRequest request, ErrorState errorState) {
        String grantType = request.getGrantType();

        if (StringUtils.isBlank(grantType)) {
            errorState.addError(
                    "grant_type",
                    "Required fields cannot be empty");
            return;
        }

        if (!"client_credentials".equals(grantType)) {
            errorState.addError(
                    "grant_type",
                    "The value is unknown. Acceptable value 'client_credentials'");
        }
    }

    public static void verifyClientId(ClientCredentialsRequest request, ErrorState errorState) {
        String clientId = request.getClientId();

        if (StringUtils.isBlank(clientId)) {
            errorState.addError(
                    "client_id",
                    "Required fields cannot be empty");
        }
    }

    public static void verifyClientSecret(ClientCredentialsRequest request, ErrorState errorState) {
        String clientSecret = request.getClientSecret();

        if (StringUtils.isBlank(clientSecret)) {
            errorState.addError(
                    "client_secret",
                    "Required fields cannot be empty");
        }
    }

    public static void verifyUserName(ChangePasswordRequest request, ErrorState errorState) {
        String username = request.getUsername();

        if (StringUtils.isBlank(username)) {
            errorState.addError(
                    "username",
                    "Required fields cannot be empty");
        }
    }

    public static void verifyPreviousPassword(ChangePasswordRequest request, ErrorState errorState) {
        String previousPassword = request.getPreviousPassword();

        if (StringUtils.isBlank(previousPassword)) {
            errorState.addError(
                    "previous_password",
                    "Required fields cannot be empty");
        }
    }

    public static void verifyProposedPassword(ChangePasswordRequest request, ErrorState errorState) {
        String proposedPassword = request.getProposedPassword();

        if (StringUtils.isBlank(proposedPassword)) {
            errorState.addError(
                    "proposed_password",
                    "Required fields cannot be empty");
        }
    }

}
