package authorization.jwt.core;

import com.azure.resourcemanager.authorization.models.RoleDefinition;

import java.util.List;

public interface PolicyRepository {

    // Returns a list of Azure Role Definitions (permissions) based on JWT claims
    List<RoleDefinition> getRoleDefinitions(Claims claims);

}