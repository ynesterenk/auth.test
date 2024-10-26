package jwt.infrastructure.azure.iam;

import authorization.jwt.infrastructure.azure.iam.AzureResourceManagerWrapper;
import authorization.jwt.infrastructure.azure.iam.RolePolicyRepository;
import com.azure.resourcemanager.authorization.models.Permission;
import com.azure.resourcemanager.authorization.models.RoleDefinition;
import authorization.jwt.core.Claims;
import authorization.jwt.core.PolicyRepository;
import org.mockito.*;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class RolePolicyRepositoryTest {

    @Mock
    private AzureResourceManagerWrapper mockAzureResourceManagerWrapper;
    @Mock
    private RoleDefinition mockRoleDefinition;
    @Mock
    private Permission mockPermission;

    private PolicyRepository repository;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        repository = new RolePolicyRepository(mockAzureResourceManagerWrapper);
    }

    @Test
    public void testGetRoleDefinitions() {
        Claims claims = new Claims();
        claims.setUsername("foo");
        claims.setRoles(Arrays.asList(
                "/subscriptions/12345678/resourceGroups/foo-group/providers/Microsoft.Authorization/roleDefinitions/foo-role-id",
                "/subscriptions/87654321/resourceGroups/bar-group/providers/Microsoft.Authorization/roleDefinitions/bar-role-id"));
        claims.setExpiredAt(
                ZonedDateTime.of(2009, 2, 13, 23, 31, 30, 0, ZoneId.of("UTC")));

        // Mock RoleDefinitions for Azure
        Mockito.when(mockAzureResourceManagerWrapper.getRoleDefinitionById(Mockito.anyString()))
                .thenReturn(mockRoleDefinition);
        Mockito.when(mockRoleDefinition.roleName()).thenReturn("foo-role", "bar-role");
        Mockito.when(mockRoleDefinition.permissions()).thenReturn(new HashSet<>(Arrays.asList(mockPermission)));
        Mockito.when(mockPermission.actions()).thenReturn((Arrays.asList("Microsoft.Compute/virtualMachines/read")));
        Mockito.when(mockPermission.notActions()).thenReturn(Arrays.asList("Microsoft.Compute/virtualMachines/delete"));

        List<RoleDefinition> roleDefinitions = repository.getRoleDefinitions(claims);

        Assert.assertNotNull(roleDefinitions);
        Assert.assertEquals(roleDefinitions.size(), 2);
        Mockito.verify(mockAzureResourceManagerWrapper, Mockito.times(2)).getRoleDefinitionById(Mockito.anyString());

        // Verify the role names match what was expected
        List<String> roleNames = roleDefinitions.stream().map(RoleDefinition::roleName).collect(Collectors.toList());
        Assert.assertEquals(roleNames, Arrays.asList("foo-role", "bar-role"));
    }

    @Test
    public void testGetUserRoleDefinitions() {
        Claims claims = new Claims();
        claims.setUsername("foo");
        claims.setRoles(Collections.singletonList(
                "/subscriptions/12345678/resourceGroups/foo-group/providers/Microsoft.Authorization/roleDefinitions/foo-role-id"));
        claims.setExpiredAt(
                ZonedDateTime.of(2009, 2, 13, 23, 31, 30, 0, ZoneId.of("UTC")));

        Mockito.when(mockAzureResourceManagerWrapper.getRoleDefinitionById(Mockito.anyString()))
                .thenReturn(mockRoleDefinition);
        Mockito.when(mockRoleDefinition.roleName()).thenReturn("foo-role");
        Mockito.when(mockRoleDefinition.permissions()).thenReturn(new HashSet<>(Arrays.asList(mockPermission)));
        Mockito.when(mockPermission.actions()).thenReturn(Arrays.asList("Microsoft.Compute/virtualMachines/read"));
        Mockito.when(mockPermission.notActions()).thenReturn(Arrays.asList("Microsoft.Compute/virtualMachines/delete"));

        List<RoleDefinition> roleDefinitions = repository.getRoleDefinitions(claims);

        Assert.assertNotNull(roleDefinitions);
        Assert.assertEquals(roleDefinitions.size(), 1);
        Assert.assertEquals(roleDefinitions.get(0).roleName(), "foo-role");

        Mockito.verify(mockAzureResourceManagerWrapper, Mockito.times(1)).getRoleDefinitionById(Mockito.anyString());
    }
}
