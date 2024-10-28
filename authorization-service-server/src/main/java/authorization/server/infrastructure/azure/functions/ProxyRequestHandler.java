package authorization.server.infrastructure.azure.functions;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import authorization.server.core.Facade;
import authorization.server.core.Route;
import authorization.server.core.Router;
import authorization.server.infrastructure.azure.Factory;
import shared.core.validation.ValidationException;
import shared.infrastructure.azure.gateway.proxy.HttpError;
import shared.infrastructure.azure.gateway.proxy.HttpRequestTranslator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@RequiredArgsConstructor
public class ProxyRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyRequestHandler.class);

    private final Factory factory;

    public ProxyRequestHandler() {
        this(Factory.getInstance());
    }

    @FunctionName("ProxyRequestHandler")
    public HttpResponseMessage handleRequest(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST, HttpMethod.GET}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {

        logger.debug("Invoked function with request: {}", request.getBody().orElse("No body"));
        final HttpRequestTranslator httpRequest = HttpRequestTranslator.ofNullable(request);

        final Route route = Router.match(httpRequest);
        final Facade facade;
        switch (route) {
            case CHANGE_PASSWORD:
                facade = factory.createChangePasswordFacade();
                break;

            case OAUTH2_CLIENT_CREDENTIALS:
                facade = factory.createClientCredentialsFacade();
                break;

            case NOT_FOUND:
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body(new HttpError("Not Found", context.getInvocationId()))
                        .build();

            default:
                throw new IllegalStateException("Unhandled route");
        }

        try {
            return facade.process(httpRequest);
        } catch (ValidationException e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(new HttpError("Validation error", context.getInvocationId(), e.getErrorState()))
                    .build();
        } catch (Exception e) {
            logger.error("Internal Server Error", e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HttpError("Internal Server Error", context.getInvocationId()))
                    .build();
        }
    }
}
