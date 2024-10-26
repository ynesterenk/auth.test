package authorization.server.core.translator;

import authorization.server.core.model.ClientCredentialsRequest;
import shared.infrastructure.azure.gateway.proxy.HttpRequestTranslator;
import shared.core.http.BasicAuthenticationException;
import shared.core.http.BasicScheme;
import shared.core.http.Credentials;
import shared.core.http.FormUrlencodedScheme;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class ClientCredentialsRequestTranslator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ClientCredentialsRequest from(HttpRequestTranslator httpRequest) {
        // Decode the request body if form-urlencoded
        HttpRequestTranslator decodedRequest = FormUrlencodedScheme.decode(httpRequest);

        // Parse JSON body to ClientCredentialsRequest
        ClientCredentialsRequest request;
        try {
            String body = decodedRequest.getBody();
            request = objectMapper.readValue(
                    (body == null || body.isEmpty()) ? "{}" : body,
                    ClientCredentialsRequest.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse ClientCredentialsRequest", e);
        }

        // Check for clientId and clientSecret in JSON body
        if (request.getClientId() != null && !request.getClientId().isEmpty() &&
                request.getClientSecret() != null && !request.getClientSecret().isEmpty()) {
            return request;
        }

        // Attempt to retrieve clientId and clientSecret from Basic Auth header
        try {
            Credentials credentials = BasicScheme.decode(decodedRequest.getHeaders());
            request.setClientId(credentials.getUsername());
            request.setClientSecret(credentials.getPassword());
        } catch (BasicAuthenticationException ignored) {
        }

        return request;
    }
}
