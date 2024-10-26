package authorization.server.core.facade;

import authorization.server.core.Facade;
import authorization.server.core.UserPool;
import authorization.server.core.UserPoolException;
import authorization.server.core.model.ClientCredentialsRequest;
import authorization.server.core.model.ClientCredentialsResponse;
import authorization.server.core.translator.ClientCredentialsRequestTranslator;
import lombok.extern.slf4j.Slf4j;
import shared.core.validation.ErrorState;
import shared.core.validation.Rule;
import shared.core.validation.ValidationException;
import shared.infrastructure.azure.gateway.proxy.HttpRequestTranslator;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.apache.http.HttpHeaders;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;

@Slf4j
public class ClientCredentialsFacade implements Facade {

    private final ErrorState errorState;
    private final UserPool userPool;
    private final Collection<Rule<HttpRequestTranslator>> preRules;
    private final Collection<Rule<ClientCredentialsRequest>> postRules;

    public ClientCredentialsFacade(ErrorState errorState, UserPool userPool,
                                   Collection<Rule<HttpRequestTranslator>> preRules,
                                   Collection<Rule<ClientCredentialsRequest>> postRules) {
        this.errorState = errorState;
        this.userPool = userPool;
        this.preRules = preRules;
        this.postRules = postRules;
    }

    @Override
    public HttpResponseMessage process(HttpRequestTranslator request) {
        // Apply pre-validation rules
        preRules.forEach(rule -> rule.accept(request, errorState));
        if (errorState.hasErrors()) {
            throw new ValidationException(errorState);
        }

        try {
            // Translate the request and process the client credentials
            ClientCredentialsRequest clientCredentialsRequest = ClientCredentialsRequestTranslator.from(request);
            ClientCredentialsResponse response = process(clientCredentialsRequest);

            return request.createResponseBuilder(HttpStatus.OK)
                    .body(response)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .build();

        } catch (UserPoolException e) {
            // Handle user pool exceptions and create an error response
            ErrorState errorState = new ErrorState();
            errorState.addError("authorization", e.getMessage());
            log.info("authorization"+e.getMessage());

            return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
                    .body(errorState)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .build();
        }
    }

    private ClientCredentialsResponse process(ClientCredentialsRequest request) throws UserPoolException {
        // Apply post-validation rules
        postRules.forEach(rule -> rule.accept(request, errorState));
        if (errorState.hasErrors()) {
            throw new ValidationException(errorState);
        }

        // Authenticate and generate access token
        String accessToken = userPool.authenticate(
                request.getClientId(), request.getClientSecret());

        ClientCredentialsResponse response = new ClientCredentialsResponse();
        response.setTokenType("Bearer");
        response.setAccessToken(accessToken);
        response.setExpiresIn(ZonedDateTime.now(ZoneId.of("UTC")).plusHours(1).toEpochSecond());
        return response;
    }
}
