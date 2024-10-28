package shared.infrastructure.azure.gateway;

import com.azure.resourcemanager.authorization.models.RoleDefinition;

import java.time.ZonedDateTime;
import java.util.List;


public class Policy {

    private final List<RoleDefinition> roleDefinitions;
    private ZonedDateTime expiration;

    public Policy(){
        this.roleDefinitions = null;
    }

    Policy(List<RoleDefinition> roleDefinitions) {
        this.roleDefinitions = roleDefinitions;
    }

    public void setExpiration(ZonedDateTime expiration) {
        this.expiration = expiration;
    }

    // Getter methods
    public List<RoleDefinition> getRoleDefinitions() {
        return roleDefinitions;
    }

    public ZonedDateTime getExpiration() {
        return expiration;
    }

    public String toJson() {
        return "";
    }
}
