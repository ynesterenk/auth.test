package shared.infrastructure.azure.gateway;

import com.azure.resourcemanager.authorization.models.RoleDefinition;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthorizerResponse {

    private final String principalId;
    private final Map<?, ?> policyDocument;
    private final Map<String, ?> context;

    public static class Builder {

        private String principalId;
        private RoleDefinition roleDefinition;
        private Map<String, Object> context = new LinkedHashMap<>();

        public Builder withPrincipalId(String principalId) {
            this.principalId = principalId;
            return this;
        }

        // Replaces withRoleDefinition to handle Azure RoleDefinition
        public Builder withRoleDefinition(RoleDefinition roleDefinition) {
            this.roleDefinition = roleDefinition;
            return this;
        }

        public Builder withContextAsString(String key, String value) {
            return withContext(key, value);
        }

        public Builder withContextAsNumber(String key, Integer value) {
            return withContext(key, value);
        }

        public Builder withContextAsNumber(String key, Long value) {
            return withContext(key, value);
        }

        public Builder withContextAsNumber(String key, Double value) {
            return withContext(key, value);
        }

        public Builder withContextAsBoolean(String key, Boolean value) {
            return withContext(key, value);
        }

        private Builder withContext(String key, Object value) {
            this.context.put(key, value);
            return this;
        }

        public AuthorizerResponse build() {
            // Convert RoleDefinition to a policy-like document (as a map)
            Map<String, Object> policyDocument = roleDefinitionToPolicyDocument(roleDefinition);

            return new AuthorizerResponse(
                    principalId,
                    Collections.unmodifiableMap(policyDocument),
                    Collections.unmodifiableMap(context));
        }

        // Method to convert RoleDefinition to a JSON-like policy document
        private Map<String, Object> roleDefinitionToPolicyDocument(RoleDefinition roleDefinition) {
            Map<String, Object> policyDocument = new LinkedHashMap<>();

            // Example mapping of RoleDefinition's actions to a policy-like structure
            policyDocument.put("roleName", roleDefinition.roleName());
            policyDocument.put("assignableScopes", roleDefinition.assignableScopes());

            // Map permissions (actions and notActions)
            policyDocument.put("permissions", roleDefinition.permissions().stream().map(permission -> {
                Map<String, Object> permissionMap = new LinkedHashMap<>();
                permissionMap.put("actions", permission.actions());
                permissionMap.put("notActions", permission.notActions());
                return permissionMap;
            }).toArray());

            return policyDocument;
        }

    }

}
