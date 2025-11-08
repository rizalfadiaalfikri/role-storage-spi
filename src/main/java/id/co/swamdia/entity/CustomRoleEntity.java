package id.co.swamdia.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "custom_roles", uniqueConstraints = @UniqueConstraint(columnNames = { "realm_id", "name", "client_id" }))
@NamedQueries({
        @NamedQuery(name = "CustomRoleEntity.findByRealm", query = "SELECT r FROM CustomRoleEntity r WHERE r.realmId = :realmId AND r.clientId IS NULL"),
        @NamedQuery(name = "CustomRoleEntity.findByRealmAndClient", query = "SELECT r FROM CustomRoleEntity r WHERE r.realmId = :realmId AND r.clientId = :clientId"),
        @NamedQuery(name = "CustomRoleEntity.findByNameAndRealm", query = "SELECT r FROM CustomRoleEntity r WHERE r.name = :name AND r.realmId = :realmId AND r.clientId IS NULL"),
        @NamedQuery(name = "CustomRoleEntity.findByNameAndRealmAndClient", query = "SELECT r FROM CustomRoleEntity r WHERE r.name = :name AND r.realmId = :realmId AND r.clientId = :clientId"),
        @NamedQuery(name = "CustomRoleEntity.search", query = "SELECT r FROM CustomRoleEntity r WHERE r.realmId = :realmId AND "
                +
                "(LOWER(r.name) LIKE LOWER(:search) OR LOWER(r.description) LIKE LOWER(:search))"),
        @NamedQuery(name = "CustomRoleEntity.countByRealm", query = "SELECT COUNT(r) FROM CustomRoleEntity r WHERE r.realmId = :realmId")
})
public class CustomRoleEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "realm_id", nullable = false, length = 255)
    private String realmId;

    @Column(name = "client_id", length = 255)
    private String clientId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public CustomRoleEntity() {
    }

    public CustomRoleEntity(String id, String name, String realmId) {
        this.id = id;
        this.name = name;
        this.realmId = realmId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CustomRoleEntity))
            return false;
        CustomRoleEntity that = (CustomRoleEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
