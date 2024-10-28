package authorization.jwt.infrastructure.azure.rbac;


import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.authorization.models.RoleDefinition;

public class AzureResourceManagerWrapper {

    private final AzureResourceManager azureResourceManager;

    public AzureResourceManagerWrapper(AzureResourceManager azureResourceManager) {
        this.azureResourceManager = azureResourceManager;
    }

    public RoleDefinition getRoleDefinitionById(String roleId) {
        return azureResourceManager.accessManagement().roleDefinitions().getById(roleId);
    }
}
