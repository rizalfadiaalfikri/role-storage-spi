package id.co.swamdia.event;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import id.co.swamdia.service.DatabaseService;

import org.jboss.logging.Logger;

/**
 * Factory untuk Role Event Listener
 * Event Listener ini akan menangkap event role creation/update/delete
 * dan menyinkronkannya ke database external
 */
public class RoleEventListenerFactory implements EventListenerProviderFactory {

    private static final Logger logger = Logger.getLogger(RoleEventListenerFactory.class);
    private static final String PROVIDER_ID = "custom-role-event-listener";

    private DatabaseService databaseService;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        logger.infof("Creating RoleEventListener for session");
        
        // Initialize database service if not already done
        if (databaseService == null) {
            logger.info("Database service not initialized, initializing from system properties");
            initializeDatabaseService();
        }

        return new RoleEventListener(session, databaseService);
    }

    @Override
    public void init(Config.Scope config) {
        logger.info("Initializing RoleEventListenerFactory");
        initializeDatabaseService();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        logger.info("Post-initialization of RoleEventListenerFactory");
    }

    @Override
    public void close() {
        logger.info("Closing RoleEventListenerFactory");
        if (databaseService != null) {
            databaseService.close();
        }
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    private void initializeDatabaseService() {
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

                logger.infof("Initializing database service from system properties. URL: %s, Username: %s", 
                        dbUrl, dbUsername);
                databaseService = new DatabaseService(dbUrl, dbUsername, dbPassword, dbDriver);
                logger.info("Database service initialized successfully from system properties");
            } else {
                logger.warn("System properties not found for database configuration. " +
                        "Event listener will not be able to sync roles to external database.");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize database service from system properties", e);
        }
    }
}

