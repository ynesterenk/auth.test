package shared.infrastructure.azure.gateway.proxy;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class HttpError {
    private String message;
    private String requestId;
    private Object details;
    private final Map<Object, Object> errors = new HashMap<>();

    public HttpError(String message, String requestId) {
        this(message, requestId, null);
    }



    public void addError(Object key, Object value) {
        errors.put(key, value);
    }

    public Map<Object, Object> getErrors() {
        // Return a copy of the errors map to avoid external modification
        return new HashMap<>(errors);
    }

}
