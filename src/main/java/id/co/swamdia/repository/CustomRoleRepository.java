package id.co.swamdia.repository;

import org.jboss.logging.Logger;

import id.co.swamdia.entity.CustomRoleEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

public class CustomRoleRepository {
    private static final Logger logger = Logger.getLogger(CustomRoleRepository.class);

    private final EntityManager entityManager;

    public CustomRoleRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // Basic CRUD operations
    public Optional<CustomRoleEntity> findById(String id) {
        try {
            CustomRoleEntity role = entityManager.find(CustomRoleEntity.class, id);
            return Optional.ofNullable(role);
        } catch (Exception e) {
            logger.error("Error finding role by ID: " + id, e);
            return Optional.empty();
        }
    }

    public List<CustomRoleEntity> findByRealm(String realmId) {
        try {
            TypedQuery<CustomRoleEntity> query = entityManager.createNamedQuery(
                    "CustomRoleEntity.findByRealm", CustomRoleEntity.class);
            query.setParameter("realmId", realmId);
            return query.getResultList();
        } catch (Exception e) {
            logger.error("Error finding roles by realm: " + realmId, e);
            return List.of();
        }
    }

    public List<CustomRoleEntity> findByRealmAndClient(String realmId, String clientId) {
        try {
            TypedQuery<CustomRoleEntity> query = entityManager.createNamedQuery(
                    "CustomRoleEntity.findByRealmAndClient", CustomRoleEntity.class);
            query.setParameter("realmId", realmId);
            query.setParameter("clientId", clientId);
            return query.getResultList();
        } catch (Exception e) {
            logger.error("Error finding roles by realm and client", e);
            return List.of();
        }
    }

    public Optional<CustomRoleEntity> findByNameAndRealm(String name, String realmId) {
        try {
            TypedQuery<CustomRoleEntity> query = entityManager.createNamedQuery(
                    "CustomRoleEntity.findByNameAndRealm", CustomRoleEntity.class);
            query.setParameter("name", name);
            query.setParameter("realmId", realmId);
            return query.getResultStream().findFirst();
        } catch (Exception e) {
            logger.error("Error finding role by name and realm", e);
            return Optional.empty();
        }
    }

    public Optional<CustomRoleEntity> findByNameAndRealmAndClient(String name, String realmId, String clientId) {
        try {
            TypedQuery<CustomRoleEntity> query = entityManager.createNamedQuery(
                    "CustomRoleEntity.findByNameAndRealmAndClient", CustomRoleEntity.class);
            query.setParameter("name", name);
            query.setParameter("realmId", realmId);
            query.setParameter("clientId", clientId);
            return query.getResultStream().findFirst();
        } catch (Exception e) {
            logger.error("Error finding role by name, realm and client", e);
            return Optional.empty();
        }
    }

    public List<CustomRoleEntity> search(String realmId, String searchTerm) {
        try {
            TypedQuery<CustomRoleEntity> query = entityManager.createNamedQuery(
                    "CustomRoleEntity.search", CustomRoleEntity.class);
            query.setParameter("realmId", realmId);
            query.setParameter("search", "%" + searchTerm + "%");
            return query.getResultList();
        } catch (Exception e) {
            logger.error("Error searching roles", e);
            return List.of();
        }
    }

    public long countByRealm(String realmId) {
        try {
            TypedQuery<Long> query = entityManager.createNamedQuery(
                    "CustomRoleEntity.countByRealm", Long.class);
            query.setParameter("realmId", realmId);
            return query.getSingleResult();
        } catch (Exception e) {
            logger.error("Error counting roles by realm", e);
            return 0;
        }
    }

    // Save operations with transaction management
    public CustomRoleEntity save(CustomRoleEntity role) {
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();

            if (role.getId() == null) {
                // Generate UUID if not provided
                role.setId(java.util.UUID.randomUUID().toString());
                entityManager.persist(role);
            } else {
                role = entityManager.merge(role);
            }

            entityManager.flush();
            transaction.commit();

            logger.infof("Saved role: %s in realm: %s", role.getName(), role.getRealmId());
            return role;

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("Error saving role", e);
            throw new RuntimeException("Failed to save role", e);
        }
    }

    public boolean delete(String id) {
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();

            CustomRoleEntity role = entityManager.find(CustomRoleEntity.class, id);
            if (role != null) {
                entityManager.remove(role);
                entityManager.flush();
                transaction.commit();
                logger.infof("Deleted role: %s", role.getName());
                return true;
            }

            if (transaction.isActive()) {
                transaction.rollback();
            }
            return false;

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("Error deleting role: " + id, e);
            return false;
        }
    }
}
