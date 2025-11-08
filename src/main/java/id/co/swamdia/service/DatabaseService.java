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

import jakarta.persistence.EntityManagerFactory;
import java.util.Properties;

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
            
            // Create EntityManagerFactory wrapper for Hibernate 6
            // Hibernate 6 uses SessionFactory, we wrap it to provide EntityManagerFactory interface
            this.entityManagerFactory = new SessionFactoryEntityManagerFactory(sessionFactory);

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
}
