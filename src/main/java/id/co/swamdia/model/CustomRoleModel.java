package id.co.swamdia.model;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.storage.StorageId;

import id.co.swamdia.entity.CustomRoleEntity;
import id.co.swamdia.repository.CustomRoleRepository;

import org.jboss.logging.Logger;

import java.util.*;
import java.util.stream.Stream;

public class CustomRoleModel implements RoleModel {
    private static final Logger logger = Logger.getLogger(CustomRoleModel.class);

    private final CustomRoleEntity entity;
    private final RealmModel realm;
    private final ComponentModel model;
    private final CustomRoleRepository roleRepository;

    public CustomRoleModel(CustomRoleEntity entity, RealmModel realm, KeycloakSession session,
            ComponentModel model, CustomRoleRepository roleRepository) {
        this.entity = entity;
        this.realm = realm;
        this.model = model;
        this.roleRepository = roleRepository;
    }

    @Override
    public String getId() {
        return new StorageId(model.getId(), entity.getId()).getId();
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public void setName(String name) {
        logger.infof("Updating role name from %s to %s", entity.getName(), name);
        entity.setName(name);
        roleRepository.save(entity);
    }

    @Override
    public String getDescription() {
        return entity.getDescription();
    }

    @Override
    public void setDescription(String description) {
        logger.infof("Updating role description for role: %s", entity.getName());
        entity.setDescription(description);
        roleRepository.save(entity);
    }

    // Tidak support composite roles
    @Override
    public boolean isComposite() {
        return false;
    }

    // Tidak support composite roles
    @Override
    public void addCompositeRole(RoleModel role) {
        throw new UnsupportedOperationException("Composite roles are not supported in this simple implementation");
    }

    // Tidak support composite roles
    @Override
    public void removeCompositeRole(RoleModel role) {
        throw new UnsupportedOperationException("Composite roles are not supported in this simple implementation");
    }

    // Tidak support composite roles
    @Override
    public Stream<RoleModel> getCompositesStream(String search, Integer first, Integer max) {
        return Stream.empty();
    }

    @Override
    public boolean isClientRole() {
        return entity.getClientId() != null;
    }

    @Override
    public String getContainerId() {
        return isClientRole() ? entity.getClientId() : realm.getId();
    }

    @Override
    public RoleContainerModel getContainer() {
        if (isClientRole()) {
            ClientModel client = realm.getClientById(entity.getClientId());
            if (client == null) {
                logger.warnf("Client not found for ID: %s", entity.getClientId());
                return null;
            }
            return client;
        } else {
            return realm;
        }
    }

    // Simple implementation - hanya check equality
    @Override
    public boolean hasRole(RoleModel role) {
        return this.equals(role);
    }

    // Tidak support attributes
    @Override
    public void setSingleAttribute(String name, String value) {
        throw new UnsupportedOperationException("Attributes are not supported in this simple implementation");
    }

    // Tidak support attributes
    @Override
    public void setAttribute(String name, List<String> values) {
        throw new UnsupportedOperationException("Attributes are not supported in this simple implementation");
    }

    // Tidak support attributes
    @Override
    public void removeAttribute(String name) {
        throw new UnsupportedOperationException("Attributes are not supported in this simple implementation");
    }

    // Tidak support attributes
    @Override
    public Stream<String> getAttributeStream(String name) {
        return Stream.empty();
    }

    // Tidak support attributes
    @Override
    public Map<String, List<String>> getAttributes() {
        return Collections.emptyMap();
    }

    public CustomRoleEntity getEntity() {
        return entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CustomRoleModel))
            return false;
        CustomRoleModel that = (CustomRoleModel) o;
        return Objects.equals(entity.getId(), that.entity.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity.getId());
    }
}
