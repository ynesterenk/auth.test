package authorization.jwt.infrastructure.azure.iam;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.authorization.models.RoleDefinition;
import authorization.jwt.core.Claims;
import authorization.jwt.core.PolicyRepository;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//@RequiredArgsConstructor
public class RolePolicyRepository implements PolicyRepository {

    private static final Pattern ROLE_ID_PATTERN = Pattern.compile(
            "/subscriptions/.*/resourceGroups/.*/providers/Microsoft.Authorization/roleDefinitions/(?<roleId>[-A-Za-z0-9+=,.@_]+)");

    private final AzureResourceManagerWrapper azureResourceManagerWrapper;

    public RolePolicyRepository(AzureResourceManagerWrapper azureResourceManagerWrapper) {
        this.azureResourceManagerWrapper = azureResourceManagerWrapper;
    }

    @Override
    public List<RoleDefinition> getRoleDefinitions(Claims claims) {
        return claims.getRoles()
                .stream()
                .flatMap(RolePolicyRepository::retrieveRoleId)
                .map(this::getRoleDefinition)
                .collect(Collectors.toList());
    }

    private RoleDefinition getRoleDefinition(String roleId) {
        return azureResourceManagerWrapper.getRoleDefinitionById(roleId);
    }

    private static Stream<String> retrieveRoleId(String roleIdPath) {
        Matcher matcher = ROLE_ID_PATTERN.matcher(roleIdPath);
        if (matcher.matches()) {
            return Stream.of(matcher.group("roleId"));
        }

        return Stream.empty();
    }

}
