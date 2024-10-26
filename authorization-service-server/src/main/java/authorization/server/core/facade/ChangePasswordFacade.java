package authorization.server.core.facade;

import authorization.server.core.Facade;
import authorization.server.core.UserPool;
import authorization.server.core.UserPoolException;
import authorization.server.core.model.ChangePasswordRequest;
import authorization.server.core.model.ChangePasswordResponse;
import authorization.server.core.translator.ChangePasswordRequestTranslator;
import com.microsoft.azure.functions.HttpResponseMessage;
import lombok.extern.slf4j.Slf4j;
import shared.core.validation.ErrorState;
import shared.core.validation.Rule;
import shared.core.validation.ValidationException;
import shared.infrastructure.azure.gateway.proxy.HttpRequestTranslator;
import com.microsoft.azure.functions.HttpStatus;
import org.apache.http.HttpHeaders;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

@Slf4j
public class ChangePasswordFacade implements Facade {

    private final ErrorState errorState;
    private final UserPool userPool;
    private final Template template;
    private final Collection<Rule<HttpRequestTranslator>> preRules;
    private final Collection<Rule<ChangePasswordRequest>> postRules;

    public ChangePasswordFacade(ErrorState errorState, UserPool userPool, Template template,
                                Collection<Rule<HttpRequestTranslator>> preRules,
                                Collection<Rule<ChangePasswordRequest>> postRules) {
        this.errorState = errorState;
        this.userPool = userPool;
        this.template = template;
        this.preRules = preRules;
        this.postRules = postRules;
    }

    @Override
    public HttpResponseMessage process(HttpRequestTranslator request)  throws UnsupportedEncodingException {
        if ("POST".equals(request.getHttpMethod())) {
            // Run pre-validation rules
            preRules.forEach(rule -> rule.accept(request, errorState));
            if (errorState.hasErrors()) {
                throw new ValidationException(errorState);
            }

            // Translate the request and process the password change
            ChangePasswordResponse response = process(
                    ChangePasswordRequestTranslator.from(request));

            HttpResponseMessage.Builder responseMessageBuilder = request.createResponseBuilder(HttpStatus.OK);
                log.info("Builder: "+responseMessageBuilder);
               return responseMessageBuilder.body(response)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .build();

        }

        // Render HTML template for non-POST requests
        VelocityContext context = new VelocityContext();
        StringWriter writer = new StringWriter();
        template.merge(context, writer);

        return request.createResponseBuilder(HttpStatus.OK)
                .body(writer.toString())
                .header("Content-Type", "text/html; charset=utf-8")
                .build();
    }

    ChangePasswordResponse process(ChangePasswordRequest request) {
        // Run post-validation rules
        postRules.forEach(rule -> rule.accept(request, errorState));
        if (errorState.hasErrors()) {
            throw new ValidationException(errorState);
        }

        ChangePasswordResponse response = new ChangePasswordResponse();
        response.setAcknowledged(false);

        try {
            userPool.changePassword(
                    request.getUsername(), request.getPreviousPassword(), request.getProposedPassword());

            response.setAcknowledged(true);
            response.setMessage("Your password has been changed successfully!");
        } catch (UserPoolException e) {
            response.setMessage(e.getMessage());
        }

        return response;
    }
}
