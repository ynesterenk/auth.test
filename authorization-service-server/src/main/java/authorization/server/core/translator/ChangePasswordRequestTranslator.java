package authorization.server.core.translator;

import authorization.server.core.model.ChangePasswordRequest;
import shared.infrastructure.azure.gateway.proxy.HttpRequestTranslator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ChangePasswordRequestTranslator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ChangePasswordRequest from(HttpRequestTranslator httpRequest)  throws UnsupportedEncodingException {
            String body = httpRequest.getBody();
            Map<String, String> params = decodeFormData(body);

            ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
            changePasswordRequest.setUsername(params.get("username"));
            changePasswordRequest.setPreviousPassword(params.get("previous_password"));
            changePasswordRequest.setProposedPassword(params.get("proposed_password"));
        return changePasswordRequest;
    }

    private static Map<String, String> decodeFormData(String body)  throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] keyValue = pair.split("=");
            String key = URLDecoder.decode(keyValue[0], "UTF-8");
            String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], "UTF-8") : "";
            params.put(key, value);
        }
        return params;
    }
}
