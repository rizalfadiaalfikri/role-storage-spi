package id.co.swamdia.provider;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.role.RoleStorageProviderFactory;

import id.co.swamdia.service.DatabaseService;

import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

public class CustomRoleStorageProviderFactory implements RoleStorageProviderFactory<CustomRoleStorageProvider> {

    private static final Logger logger = Logger.getLogger(CustomRoleStorageProviderFactory.class);
    private static final String PROVIDER_ID = "simple-mysql-role-storage";

    // Configuration properties
    private static final String DB_URL = "dbUrl";
    private static final String DB_USERNAME = "dbUsername";
    private static final String DB_PASSWORD = "dbPassword";
    private static final String DB_DRIVER = "dbDriver";

    private DatabaseService databaseService;

    @Override
    public CustomRoleStorageProvider create(KeycloakSession session, ComponentModel model) {
        logger.infof("Creating Simple CustomRoleStorageProvider with MySQL storage. Component ID: %s, Provider ID: %s",
                model.getId(), model.getProviderId());

        // Initialize database service if not already done
        if (databaseService == null) {
            logger.info("Database service not initialized, initializing from ComponentModel");
            initializeDatabaseService(model);
        } else {
            logger.info("Using existing database service");
        }

        CustomRoleStorageProvider provider = new CustomRoleStorageProvider(session, model, databaseService);
        logger.info("CustomRoleStorageProvider instance created successfully");
        return provider;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Simple Custom Role Storage Provider with MySQL Database";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> configProperties = new ArrayList<>();

        configProperties.add(new ProviderConfigProperty(
                DB_URL,
                "Database URL",
                "JDBC URL for the PostgreSQL database",
                ProviderConfigProperty.STRING_TYPE,
                "jdbc:postgresql://host.docker.internal:5432/user_store"));

        configProperties.add(new ProviderConfigProperty(
                DB_USERNAME,
                "Database Username",
                "Username for database connection",
                ProviderConfigProperty.STRING_TYPE,
                "postgres"));

        configProperties.add(new ProviderConfigProperty(
                DB_PASSWORD,
                "Database Password",
                "Password for database connection",
                ProviderConfigProperty.PASSWORD,
                "root"));

        configProperties.add(new ProviderConfigProperty(
                DB_DRIVER,
                "Database Driver",
                "JDBC driver class name",
                ProviderConfigProperty.STRING_TYPE,
                "org.postgresql.Driver"));

        return configProperties;
    }

    @Override
    public void init(Config.Scope config) {
        logger.info("Initializing Simple CustomRoleStorageProviderFactory");

        // Try to initialize database service from system properties if available
        // This allows provider to work even without component configuration
        try {
            String dbUrl = System.getProperty("quarkus.datasource.user-store.jdbc.url");
            String dbUsername = System.getProperty("quarkus.datasource.user-store.username");
            String dbPassword = System.getProperty("quarkus.datasource.user-store.password");
            String dbKind = System.getProperty("quarkus.datasource.user-store.db-kind");

            if (dbUrl != null && dbUsername != null) {
                String dbDriver = "org.postgresql.Driver";
                if (dbKind != null && !dbKind.contains(".")) {
                    if ("postgresql".equalsIgnoreCase(dbKind) || "postgres".equalsIgnoreCase(dbKind)) {
                        dbDriver = "org.postgresql.Driver";
                    }
                }

                logger.infof("Initializing database service from system properties. URL: %s, Username: %s", dbUrl,
                        dbUsername);
                databaseService = new DatabaseService(dbUrl, dbUsername, dbPassword, dbDriver);
                logger.info("Database service initialized successfully from system properties");
            } else {
                logger.info(
                        "System properties not found, will initialize from ComponentModel when provider is created");
            }
        } catch (Exception e) {
            logger.warn("Failed to initialize database service from system properties, will try later", e);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        logger.info("Post-initialization of Simple CustomRoleStorageProviderFactory");
        if (databaseService != null) {
            logger.info("Database service is ready");
        } else {
            logger.info("Database service will be initialized when provider component is created");
        }
    }

    @Override
    public void close() {
        logger.info("Closing Simple CustomRoleStorageProviderFactory");
        if (databaseService != null) {
            databaseService.close();
        }
    }

    private void initializeDatabaseService(ComponentModel model) {
        // Try to get from ComponentModel first, then fallback to system
        // properties/environment variables
        String dbUrl = getConfigValue(model, DB_URL,
                System.getProperty("quarkus.datasource.user-store.jdbc.url"),
                System.getenv("DB_URL"));

        String dbUsername = getConfigValue(model, DB_USERNAME,
                System.getProperty("quarkus.datasource.user-store.username"),
                System.getenv("DB_USERNAME"));

        String dbPassword = getConfigValue(model, DB_PASSWORD,
                System.getProperty("quarkus.datasource.user-store.password"),
                System.getenv("DB_PASSWORD"));

        String dbDriver = getConfigValue(model, DB_DRIVER,
                System.getProperty("quarkus.datasource.user-store.db-kind"),
                System.getenv("DB_DRIVER"),
                "org.postgresql.Driver"); // default

        // Convert db-kind to driver if needed
        if (dbDriver != null && !dbDriver.contains(".")) {
            // If it's just "postgresql" or "postgres", convert to driver class
            if ("postgresql".equalsIgnoreCase(dbDriver) || "postgres".equalsIgnoreCase(dbDriver)) {
                dbDriver = "org.postgresql.Driver";
            }
        }

        if (dbUrl == null || dbUsername == null) {
            throw new IllegalStateException("Database configuration is missing. " +
                    "Please configure via Keycloak Admin Console or set system properties: " +
                    "quarkus.datasource.user-store.jdbc.url, quarkus.datasource.user-store.username");
        }

        try {
            databaseService = new DatabaseService(dbUrl, dbUsername, dbPassword, dbDriver);
            logger.info("Database service initialized from component model");
        } catch (Exception e) {
            logger.error("Failed to initialize database service", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Helper method to get config value with fallback chain
     */
    private String getConfigValue(ComponentModel model, String key, String... fallbacks) {
        // First try ComponentModel
        String value = model.getConfig().getFirst(key);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }

        // Then try fallbacks
        for (String fallback : fallbacks) {
            if (fallback != null && !fallback.trim().isEmpty()) {
                return fallback;
            }
        }

        return null;
    }
}
