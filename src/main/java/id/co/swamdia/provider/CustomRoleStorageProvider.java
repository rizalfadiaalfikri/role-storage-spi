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
        this.session = session;
        this.model = model;
        this.databaseService = databaseService;
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
        logger.infof("Searching for realm roles with query: '%s' in realm: %s (first: %s, max: %s)",
                search, realm.getName(), first, max);

        List<CustomRoleEntity> roles;

        // If search is null or empty, get all realm roles
        if (search == null || search.trim().isEmpty()) {
            logger.info("Search term is empty, fetching all realm roles");
            roles = getRoleRepository().findByRealm(realm.getId());
        } else {
            roles = getRoleRepository().search(realm.getId(), search);
        }

        logger.infof("Found %d roles from database", roles.size());

        Stream<CustomRoleEntity> roleStream = roles.stream();
        if (first != null && first > 0) {
            roleStream = roleStream.skip(first);
        }
        if (max != null && max > 0) {
            roleStream = roleStream.limit(max);
        }

        return roleStream.map(entity -> {
            logger.debugf("Mapping role entity to RoleModel: %s", entity.getName());
            return toRoleModel(realm, entity);
        });
    }

    @Override
    public RoleModel getRealmRole(RealmModel realm, String name) {
        logger.infof("Getting realm role by name: %s in realm: %s", name, realm.getName());

        return getRoleRepository().findByNameAndRealm(name, realm.getId())
                .map(entity -> toRoleModel(realm, entity))
                .orElse(null);
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
        logger.infof("Getting role by ID: %s in realm: %s", id, realm.getName());

        // Check if this is our role
        StorageId storageId = new StorageId(id);
        if (!storageId.getProviderId().equals(model.getId())) {
            return null;
        }

        return getRoleRepository().findById(storageId.getExternalId())
                .map(entity -> toRoleModel(realm, entity))
                .orElse(null);
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
