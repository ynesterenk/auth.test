package authorization.server.core;

import shared.infrastructure.azure.gateway.proxy.HttpRequestTranslator;
import com.microsoft.azure.functions.HttpResponseMessage;

public interface Facade {

    HttpResponseMessage process(HttpRequestTranslator request) throws Exception;

}
