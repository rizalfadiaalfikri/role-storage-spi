package id.co.swamdia.provider;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.role.RoleStorageProvider;

import id.co.swamdia.entity.CustomRoleEntity;
import id.co.swamdia.model.CustomRoleModel;
import id.co.swamdia.repository.CustomRoleRepository;
import id.co.swamdia.service.DatabaseService;

import org.jboss.logging.Logger;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomRoleStorageProvider implements RoleStorageProvider {

    private static final Logger logger = Logger.getLogger(CustomRoleStorageProvider.class);

    private final KeycloakSession session;
    private final ComponentModel model;
    private final DatabaseService databaseService;
    private EntityManager entityManager;
    private CustomRoleRepository roleRepository;

    public CustomRoleStorageProvider(KeycloakSession session, ComponentModel model, DatabaseService databaseService) {
        logger.infof("=== CustomRoleStorageProvider CONSTRUCTOR CALLED ===");
        logger.infof("Component ID: %s, Provider ID: %s", model.getId(), model.getProviderId());
        logger.infof("Realm: %s",
                session.getContext().getRealm() != null ? session.getContext().getRealm().getName() : "null");
        this.session = session;
        this.model = model;
        this.databaseService = databaseService;
        logger.info("CustomRoleStorageProvider instance created successfully");
    }

    private CustomRoleRepository getRoleRepository() {
        if (roleRepository == null) {
            entityManager = databaseService.getEntityManagerFactory().createEntityManager();
            roleRepository = new CustomRoleRepository(entityManager);
        }
        return roleRepository;
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(RealmModel realm, String search, Integer first, Integer max) {
        logger.infof("=== searchForRolesStream CALLED ===");
        logger.infof("Realm: %s (ID: %s)", realm.getName(), realm.getId());
        logger.infof("Search: '%s' (null: %s, empty: %s)",
                search, search == null, search != null && search.trim().isEmpty());
        logger.infof("Pagination: first=%s, max=%s", first, max);

        List<CustomRoleEntity> roles;

        // Special case: "*" means get all roles (workaround for Keycloak not calling
        // provider for default list)
        if (search != null && search.trim().equals("*")) {
            logger.info("Search term is '*' - fetching ALL realm roles from database (special wildcard)");
            roles = getRoleRepository().findByRealm(realm.getId());
            logger.infof("Retrieved %d realm roles from database (wildcard search)", roles.size());
        }
        // If search is null or empty, get all realm roles
        else if (search == null || search.trim().isEmpty()) {
            logger.info("Search term is null or empty - fetching ALL realm roles from database");
            roles = getRoleRepository().findByRealm(realm.getId());
            logger.infof("Retrieved %d realm roles from database (no search filter)", roles.size());
        } else {
            // For search, use search method which does LIKE query
            logger.infof("Search term provided: '%s' - using search method", search);
            roles = getRoleRepository().search(realm.getId(), search);
            logger.infof("Retrieved %d realm roles matching search term '%s'", roles.size(), search);
        }

        // Log role names for debugging
        if (!roles.isEmpty()) {
            logger.infof("Roles found: %s",
                    roles.stream()
                            .map(CustomRoleEntity::getName)
                            .collect(Collectors.joining(", ")));
        } else {
            logger.warn("No roles found in database for realm: " + realm.getId());
        }

        // Apply pagination
        Stream<CustomRoleEntity> roleStream = roles.stream();
        if (first != null && first > 0) {
            logger.debugf("Skipping first %d roles", first);
            roleStream = roleStream.skip(first);
        }
        if (max != null && max > 0) {
            logger.debugf("Limiting to %d roles", max);
            roleStream = roleStream.limit(max);
        }

        // Map to RoleModel and log
        return roleStream.map(entity -> {
            logger.debugf("Mapping role entity to RoleModel: %s (ID: %s)", entity.getName(), entity.getId());
            RoleModel roleModel = toRoleModel(realm, entity);
            logger.debugf("Created RoleModel: %s (ID: %s)", roleModel.getName(), roleModel.getId());
            return roleModel;
        });
    }

    @Override
    public RoleModel getRealmRole(RealmModel realm, String name) {
        logger.infof("=== getRealmRole CALLED ===");
        logger.infof("Getting realm role by name: '%s' in realm: %s (ID: %s)",
                name, realm.getName(), realm.getId());

        return getRoleRepository().findByNameAndRealm(name, realm.getId())
                .map(entity -> {
                    logger.infof("Found role in database: %s (ID: %s)", entity.getName(), entity.getId());
                    return toRoleModel(realm, entity);
                })
                .orElseGet(() -> {
                    logger.warnf("Role not found in database: '%s' in realm: %s", name, realm.getId());
                    return null;
                });
    }

    @Override
    public RoleModel getClientRole(ClientModel client, String name) {
        logger.infof("Getting client role by name: %s for client: %s",
                name, client.getClientId());

        return getRoleRepository().findByNameAndRealmAndClient(name, client.getRealm().getId(), client.getId())
                .map(entity -> toRoleModel(client.getRealm(), entity))
                .orElse(null);
    }

    @Override
    public RoleModel getRoleById(RealmModel realm, String id) {
        logger.infof("=== getRoleById CALLED ===");
        logger.infof("Getting role by ID: %s in realm: %s (ID: %s)", id, realm.getName(), realm.getId());

        // Check if this is our role
        StorageId storageId = new StorageId(id);
        logger.debugf("StorageId - ProviderId: %s, ExternalId: %s, ComponentId: %s",
                storageId.getProviderId(), storageId.getExternalId(), model.getId());

        if (!storageId.getProviderId().equals(model.getId())) {
            logger.debugf("Role ID does not belong to this provider. ProviderId: %s, Expected: %s",
                    storageId.getProviderId(), model.getId());
            return null;
        }

        return getRoleRepository().findById(storageId.getExternalId())
                .map(entity -> {
                    logger.infof("Found role in database by ID: %s (Name: %s)", entity.getId(), entity.getName());
                    return toRoleModel(realm, entity);
                })
                .orElseGet(() -> {
                    logger.warnf("Role not found in database by ID: %s", storageId.getExternalId());
                    return null;
                });
    }

    @Override
    public Stream<RoleModel> searchForClientRolesStream(ClientModel client, String search, Integer first, Integer max) {
        logger.infof("Searching for client roles with query: %s for client: %s", search, client.getClientId());

        List<CustomRoleEntity> roles = getRoleRepository().search(client.getRealm().getId(), search);
        roles = roles.stream()
                .filter(role -> client.getId().equals(role.getClientId()))
                .collect(Collectors.toList());

        Stream<CustomRoleEntity> roleStream = roles.stream();
        if (first != null && max != null) {
            roleStream = roleStream.skip(first).limit(max);
        }

        return roleStream.map(entity -> toRoleModel(client.getRealm(), entity));
    }

    @Override
    public Stream<RoleModel> searchForClientRolesStream(RealmModel realm, String search, Stream<String> clientIds,
            Integer first, Integer max) {
        logger.infof("Searching for client roles with query: %s in realm: %s with client IDs filter", search,
                realm.getName());

        List<String> clientIdList = clientIds != null ? clientIds.collect(Collectors.toList()) : null;

        List<CustomRoleEntity> roles = getRoleRepository().search(realm.getId(), search);

        // Filter by client IDs if provided
        if (clientIdList != null && !clientIdList.isEmpty()) {
            roles = roles.stream()
                    .filter(role -> role.getClientId() != null && clientIdList.contains(role.getClientId()))
                    .collect(Collectors.toList());
        } else {
            // Only client roles (not realm roles)
            roles = roles.stream()
                    .filter(role -> role.getClientId() != null)
                    .collect(Collectors.toList());
        }

        Stream<CustomRoleEntity> roleStream = roles.stream();
        if (first != null && max != null) {
            roleStream = roleStream.skip(first != null ? first : 0).limit(max != null ? max : Integer.MAX_VALUE);
        }

        return roleStream.map(entity -> toRoleModel(realm, entity));
    }

    @Override
    public Stream<RoleModel> searchForClientRolesStream(RealmModel realm, Stream<String> clientIds, String search,
            Integer first, Integer max) {
        logger.infof(
                "Searching for client roles with query: %s in realm: %s with client IDs filter (alternative method)",
                search, realm.getName());

        List<String> clientIdList = clientIds != null ? clientIds.collect(Collectors.toList()) : null;

        List<CustomRoleEntity> roles = getRoleRepository().search(realm.getId(), search);

        // Filter by client IDs if provided
        if (clientIdList != null && !clientIdList.isEmpty()) {
            roles = roles.stream()
                    .filter(role -> role.getClientId() != null && clientIdList.contains(role.getClientId()))
                    .collect(Collectors.toList());
        } else {
            // Only client roles (not realm roles)
            roles = roles.stream()
                    .filter(role -> role.getClientId() != null)
                    .collect(Collectors.toList());
        }

        Stream<CustomRoleEntity> roleStream = roles.stream();
        if (first != null && max != null) {
            roleStream = roleStream.skip(first != null ? first : 0).limit(max != null ? max : Integer.MAX_VALUE);
        }

        return roleStream.map(entity -> toRoleModel(realm, entity));
    }

    @Override
    public void close() {
        logger.info("Closing Simple CustomRoleStorageProvider");
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
    }

    private RoleModel toRoleModel(RealmModel realm, CustomRoleEntity entity) {
        return new CustomRoleModel(entity, realm, session, model, getRoleRepository());
    }
}
