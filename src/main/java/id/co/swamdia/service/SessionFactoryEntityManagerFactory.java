package id.co.swamdia.service;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Cache;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Query;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.Map;

/**
 * Wrapper class to adapt Hibernate 6 SessionFactory to Jakarta Persistence EntityManagerFactory
 */
public class SessionFactoryEntityManagerFactory implements EntityManagerFactory {

    private final SessionFactory sessionFactory;

    public SessionFactoryEntityManagerFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public EntityManager createEntityManager() {
        // Hibernate 6: Create EntityManager from SessionFactory
        // SessionFactory in Hibernate 6 provides EntityManager through Session
        Session session = sessionFactory.openSession();
        // In Hibernate 6, we need to use Session as EntityManager
        // Session implements EntityManager interface in Hibernate 6
        return (EntityManager) session;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public EntityManager createEntityManager(Map map) {
        // Hibernate 6 doesn't support properties map in this way
        // Just create a regular EntityManager
        return createEntityManager();
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType) {
        return createEntityManager();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
        return createEntityManager();
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        Session session = sessionFactory.openSession();
        try {
            return session.getCriteriaBuilder();
        } finally {
            session.close();
        }
    }

    @Override
    public Metamodel getMetamodel() {
        return sessionFactory.getMetamodel();
    }

    @Override
    public boolean isOpen() {
        return sessionFactory != null && !sessionFactory.isClosed();
    }

    @Override
    public void close() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of();
    }

    @Override
    public Cache getCache() {
        return new Cache() {
            @Override
            @SuppressWarnings("rawtypes")
            public boolean contains(Class cls, Object primaryKey) {
                return false;
            }

            @Override
            @SuppressWarnings("rawtypes")
            public void evict(Class cls, Object primaryKey) {
                // No-op
            }

            @Override
            @SuppressWarnings("rawtypes")
            public void evict(Class cls) {
                // No-op
            }

            @Override
            public void evictAll() {
                // No-op
            }

            @Override
            public <T> T unwrap(Class<T> cls) {
                return null;
            }
        };
    }

    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        return new PersistenceUnitUtil() {
            @Override
            public Object getIdentifier(Object entity) {
                if (entity == null) {
                    return null;
                }
                Session session = sessionFactory.openSession();
                try {
                    return session.getIdentifier(entity);
                } finally {
                    session.close();
                }
            }

            @Override
            public boolean isLoaded(Object entity) {
                return true;
            }

            @Override
            public boolean isLoaded(Object entity, String attributeName) {
                return true;
            }
        };
    }

    @Override
    public void addNamedQuery(String name, Query query) {
        // No-op for Hibernate 6
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        if (cls.isInstance(sessionFactory)) {
            return cls.cast(sessionFactory);
        }
        throw new IllegalArgumentException("Cannot unwrap to " + cls.getName());
    }

    @Override
    public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
        // No-op for Hibernate 6
    }
}

