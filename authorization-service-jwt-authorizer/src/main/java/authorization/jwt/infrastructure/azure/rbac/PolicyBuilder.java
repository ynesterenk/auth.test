package authorization.jwt.infrastructure.azure.rbac;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.authorization.models.RoleDefinition;
import com.azure.resourcemanager.authorization.models.RoleAssignment;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PolicyBuilder {

    @Getter
    private final List<RoleDefinition> roleDefinitions;
    private ZonedDateTime expiredAt;

    public PolicyBuilder() {
        this.roleDefinitions = new ArrayList<>();
    }

    // Method to add Role Definitions
    PolicyBuilder withRoleDefinitions(List<RoleDefinition> roles) {
        roleDefinitions.addAll(roles);
        return this;
    }

    // Method to add expiration time
    PolicyBuilder withExpiredAt(ZonedDateTime expiredAt) {
        this.expiredAt = expiredAt;
        return this;
    }


    // Mock method to create role assignments (this would be part of your app logic)
    List<RoleAssignment> assignRoles(String principalId, String scope, AzureResourceManager azureResourceManager) {
        return roleDefinitions.stream()
                .map(roleDefinition -> azureResourceManager.accessManagement().roleAssignments()
                        .define(java.util.UUID.randomUUID().toString())
                        .forObjectId(principalId)
                        .withRoleDefinition(roleDefinition.id())
                        .withScope(scope)
                        .create())
                .collect(Collectors.toList());
    }


}
