package id.co.swamdia.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import id.co.swamdia.entity.CustomRoleEntity;

import org.jboss.logging.Logger;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.SessionFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Properties;

/**
 * Database service using JPA with Hibernate
 * Uses Hibernate SessionFactory wrapped as EntityManagerFactory for JPA
 * compatibility
 */
public class DatabaseService {
    private static final Logger logger = Logger.getLogger(DatabaseService.class);

    private HikariDataSource dataSource;
    private SessionFactory sessionFactory;
    private EntityManagerFactory entityManagerFactory;

    public DatabaseService(String jdbcUrl, String username, String password, String driverClass) {
        initializeDataSource(jdbcUrl, username, password, driverClass);
        initializeHibernate();
    }

    private void initializeDataSource(String jdbcUrl, String username, String password, String driverClass) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClass);

        // Connection pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1800000);
        config.setAutoCommit(false);

        // PostgreSQL specific settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);
        logger.info("PostgreSQL database connection pool initialized");
    }

    /**
     * Initialize Hibernate and create EntityManagerFactory
     * Uses Hibernate SessionFactory wrapped as EntityManagerFactory for JPA
     * compatibility
     */
    private void initializeHibernate() {
        try {
            Properties settings = new Properties();
            settings.put(AvailableSettings.DATASOURCE, dataSource);
            settings.put(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
            settings.put(AvailableSettings.SHOW_SQL, "false");
            settings.put(AvailableSettings.FORMAT_SQL, "true");
            settings.put(AvailableSettings.HBM2DDL_AUTO, "update");
            settings.put(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "thread");
            settings.put(AvailableSettings.USE_SECOND_LEVEL_CACHE, "false");
            settings.put(AvailableSettings.USE_QUERY_CACHE, "false");

            StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySettings(settings)
                    .build();

            MetadataSources metadataSources = new MetadataSources(serviceRegistry);
            metadataSources.addAnnotatedClass(CustomRoleEntity.class);

            this.sessionFactory = metadataSources.buildMetadata()
                    .buildSessionFactory();

            // Wrap SessionFactory as EntityManagerFactory for JPA compatibility
            // Hibernate 6 SessionFactory can be used as EntityManagerFactory
            this.entityManagerFactory = new EntityManagerFactoryWrapper(sessionFactory);

            logger.info("Hibernate EntityManagerFactory initialized for PostgreSQL");

        } catch (Exception e) {
            logger.error("Failed to initialize Hibernate with PostgreSQL", e);
            throw new RuntimeException("PostgreSQL database initialization failed", e);
        }
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public void close() {
        if (sessionFactory != null && sessionFactory.isOpen()) {
            sessionFactory.close();
        }
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        logger.info("PostgreSQL database connections closed");
    }

    /**
     * Simple wrapper to adapt Hibernate SessionFactory to EntityManagerFactory
     * In Hibernate 6, SessionFactory can be used directly as EntityManagerFactory
     */
    private static class EntityManagerFactoryWrapper implements EntityManagerFactory {
        private final SessionFactory sessionFactory;

        public EntityManagerFactoryWrapper(SessionFactory sessionFactory) {
            this.sessionFactory = sessionFactory;
        }

        @Override
        public EntityManager createEntityManager() {
            // Hibernate 6: SessionFactory.openSession() returns Session which implements
            // EntityManager
            return sessionFactory.openSession();
        }

        @Override
        @SuppressWarnings("rawtypes")
        public EntityManager createEntityManager(java.util.Map map) {
            return createEntityManager();
        }

        @Override
        public EntityManager createEntityManager(jakarta.persistence.SynchronizationType synchronizationType) {
            return createEntityManager();
        }

        @Override
        @SuppressWarnings("rawtypes")
        public EntityManager createEntityManager(jakarta.persistence.SynchronizationType synchronizationType,
                java.util.Map map) {
            return createEntityManager();
        }

        @Override
        public jakarta.persistence.criteria.CriteriaBuilder getCriteriaBuilder() {
            return sessionFactory.getCriteriaBuilder();
        }

        @Override
        public jakarta.persistence.metamodel.Metamodel getMetamodel() {
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
        public java.util.Map<String, Object> getProperties() {
            return java.util.Map.of();
        }

        @Override
        public jakarta.persistence.Cache getCache() {
            return sessionFactory.getCache();
        }

        @Override
        public jakarta.persistence.PersistenceUnitUtil getPersistenceUnitUtil() {
            return sessionFactory.getPersistenceUnitUtil();
        }

        @Override
        public void addNamedQuery(String name, jakarta.persistence.Query query) {
            // No-op
        }

        @Override
        public <T> T unwrap(Class<T> cls) {
            if (cls.isInstance(sessionFactory)) {
                return cls.cast(sessionFactory);
            }
            throw new IllegalArgumentException("Cannot unwrap to " + cls.getName());
        }

        @Override
        public <T> void addNamedEntityGraph(String graphName, jakarta.persistence.EntityGraph<T> entityGraph) {
            // No-op
        }
    }
}
