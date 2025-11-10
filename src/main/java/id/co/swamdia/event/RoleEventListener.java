package id.co.swamdia.event;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

import id.co.swamdia.entity.CustomRoleEntity;
import id.co.swamdia.repository.CustomRoleRepository;
import id.co.swamdia.service.DatabaseService;

import org.jboss.logging.Logger;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import java.util.Optional;

/**
 * Event Listener untuk menangkap event role creation/update/delete
 * dan menyinkronkannya ke database external
 */
public class RoleEventListener implements EventListenerProvider {

    private static final Logger logger = Logger.getLogger(RoleEventListener.class);

    private final KeycloakSession session;
    private final DatabaseService databaseService;

    public RoleEventListener(KeycloakSession session, DatabaseService databaseService) {
        this.session = session;
        this.databaseService = databaseService;
    }

    @Override
    public void onEvent(Event event) {
        // User events - tidak digunakan untuk role management
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        logger.infof("=== AdminEvent received ===");
        logger.infof("Operation: %s, ResourceType: %s, ResourcePath: %s", 
                adminEvent.getOperationType(), adminEvent.getResourceType(), adminEvent.getResourcePath());

        // Hanya handle role-related events
        if (adminEvent.getResourceType() != ResourceType.REALM_ROLE && 
            adminEvent.getResourceType() != ResourceType.CLIENT_ROLE) {
            return;
        }

        RealmModel realm = session.realms().getRealm(adminEvent.getRealmId());
        if (realm == null) {
            logger.warnf("Realm not found: %s", adminEvent.getRealmId());
            return;
        }

        EntityManager entityManager = null;
        try {
            entityManager = databaseService.getEntityManagerFactory().createEntityManager();
            CustomRoleRepository roleRepository = new CustomRoleRepository(entityManager);

            if (adminEvent.getOperationType() == OperationType.CREATE) {
                handleRoleCreate(adminEvent, realm, roleRepository);
            } else if (adminEvent.getOperationType() == OperationType.UPDATE) {
                handleRoleUpdate(adminEvent, realm, roleRepository);
            } else if (adminEvent.getOperationType() == OperationType.DELETE) {
                handleRoleDelete(adminEvent, realm, roleRepository);
            }

        } catch (Exception e) {
            logger.error("Error handling admin event", e);
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    private void handleRoleCreate(AdminEvent adminEvent, RealmModel realm, CustomRoleRepository roleRepository) {
        logger.infof("=== Handling ROLE CREATE event ===");
        logger.infof("ResourcePath: %s", adminEvent.getResourcePath());

        try {
            // Extract role name from resource path
            // Format: /realms/{realm}/roles/{role-name} or /realms/{realm}/clients/{client-id}/roles/{role-name}
            String resourcePath = adminEvent.getResourcePath();
            String roleName = extractRoleNameFromPath(resourcePath);
            
            if (roleName == null) {
                logger.warnf("Could not extract role name from path: %s", resourcePath);
                return;
            }

            logger.infof("Creating role in external database: %s in realm: %s", roleName, realm.getId());

            // Get role from Keycloak to get full details
            RoleModel role = realm.getRole(roleName);
            if (role == null) {
                // Try to get from client roles
                String[] parts = resourcePath.split("/");
                if (parts.length > 4 && "clients".equals(parts[3])) {
                    String clientId = parts[4];
                    var client = realm.getClientById(clientId);
                    if (client != null) {
                        role = client.getRole(roleName);
                    }
                }
            }

            if (role == null) {
                logger.warnf("Role not found in Keycloak: %s", roleName);
                return;
            }

            // Check if role already exists in external database
            boolean isClientRole = role.isClientRole();
            String clientId = isClientRole ? role.getContainerId() : null;

            var existingRole = isClientRole 
                ? roleRepository.findByNameAndRealmAndClient(roleName, realm.getId(), clientId)
                : roleRepository.findByNameAndRealm(roleName, realm.getId());

            if (existingRole.isPresent()) {
                logger.infof("Role already exists in external database: %s", roleName);
                return;
            }

            // Create new role entity
            CustomRoleEntity roleEntity = new CustomRoleEntity();
            roleEntity.setId(UUID.randomUUID().toString());
            roleEntity.setName(roleName);
            roleEntity.setDescription(role.getDescription());
            roleEntity.setRealmId(realm.getId());
            roleEntity.setClientId(clientId);

            // Save to external database
            roleRepository.save(roleEntity);
            logger.infof("Successfully created role in external database: %s (ID: %s)", 
                    roleName, roleEntity.getId());

        } catch (Exception e) {
            logger.errorf(e, "Error creating role in external database: %s", adminEvent.getResourcePath());
        }
    }

    private void handleRoleUpdate(AdminEvent adminEvent, RealmModel realm, CustomRoleRepository roleRepository) {
        logger.infof("=== Handling ROLE UPDATE event ===");
        logger.infof("ResourcePath: %s", adminEvent.getResourcePath());

        try {
            String resourcePath = adminEvent.getResourcePath();
            String roleName = extractRoleNameFromPath(resourcePath);
            
            if (roleName == null) {
                logger.warnf("Could not extract role name from path: %s", resourcePath);
                return;
            }

            // Get role from Keycloak
            RoleModel role = realm.getRole(roleName);
            if (role == null) {
                String[] parts = resourcePath.split("/");
                if (parts.length > 4 && "clients".equals(parts[3])) {
                    String clientId = parts[4];
                    var client = realm.getClientById(clientId);
                    if (client != null) {
                        role = client.getRole(roleName);
                    }
                }
            }

            if (role == null) {
                logger.warnf("Role not found in Keycloak: %s", roleName);
                return;
            }

            boolean isClientRole = role.isClientRole();
            String clientId = isClientRole ? role.getContainerId() : null;

            // Find existing role in external database
            var existingRole = isClientRole 
                ? roleRepository.findByNameAndRealmAndClient(roleName, realm.getId(), clientId)
                : roleRepository.findByNameAndRealm(roleName, realm.getId());

            if (existingRole.isEmpty()) {
                logger.warnf("Role not found in external database, creating new one: %s", roleName);
                // Create if not exists
                handleRoleCreate(adminEvent, realm, roleRepository);
                return;
            }

            // Update existing role
            CustomRoleEntity roleEntity = existingRole.get();
            roleEntity.setName(roleName);
            roleEntity.setDescription(role.getDescription());
            roleEntity.setRealmId(realm.getId());
            roleEntity.setClientId(clientId);

            roleRepository.save(roleEntity);
            logger.infof("Successfully updated role in external database: %s", roleName);

        } catch (Exception e) {
            logger.errorf(e, "Error updating role in external database: %s", adminEvent.getResourcePath());
        }
    }

    private void handleRoleDelete(AdminEvent adminEvent, RealmModel realm, CustomRoleRepository roleRepository) {
        logger.infof("=== Handling ROLE DELETE event ===");
        logger.infof("ResourcePath: %s", adminEvent.getResourcePath());

        try {
            String resourcePath = adminEvent.getResourcePath();
            String roleName = extractRoleNameFromPath(resourcePath);
            
            if (roleName == null) {
                logger.warnf("Could not extract role name from path: %s", resourcePath);
                return;
            }

            // Try to find role in external database
            // We need to check both realm and client roles
            var realmRole = roleRepository.findByNameAndRealm(roleName, realm.getId());
            
            if (realmRole.isPresent()) {
                roleRepository.delete(realmRole.get().getId());
                logger.infof("Successfully deleted realm role from external database: %s", roleName);
                return;
            }

            // Try client roles
            realm.getClientsStream().forEach(client -> {
                var clientRole = roleRepository.findByNameAndRealmAndClient(
                        roleName, realm.getId(), client.getId());
                if (clientRole.isPresent()) {
                    roleRepository.delete(clientRole.get().getId());
                    logger.infof("Successfully deleted client role from external database: %s (client: %s)", 
                            roleName, client.getClientId());
                }
            });

            logger.warnf("Role not found in external database for deletion: %s", roleName);

        } catch (Exception e) {
            logger.errorf(e, "Error deleting role from external database: %s", adminEvent.getResourcePath());
        }
    }

    private String extractRoleNameFromPath(String resourcePath) {
        // Format: /realms/{realm}/roles/{role-name}
        // or: /realms/{realm}/clients/{client-id}/roles/{role-name}
        if (resourcePath == null || resourcePath.isEmpty()) {
            return null;
        }

        String[] parts = resourcePath.split("/");
        if (parts.length >= 5 && "roles".equals(parts[parts.length - 2])) {
            return parts[parts.length - 1];
        }

        return null;
    }

    @Override
    public void close() {
        // No cleanup needed
    }
}

