package org.acme;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
// import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
// import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;
// import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Readiness
@ApplicationScoped
public class PostgresHealthCheck implements HealthCheck {

    // @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    String jdbcUrl = "jdbc:postgresql://postgres:5432/mensageria_DB";

    // @ConfigProperty(name = "quarkus.datasource.username")
    String username = "admin";

    // @ConfigProperty(name = "quarkus.datasource.password")
    String password = "123";

    private static final Logger LOGGER = Logger.getLogger(PostgresHealthCheck.class);

    @Override
    public HealthCheckResponse call() {
        boolean isUp = checkServiceHealth();

        if (isUp) {
            LOGGER.info("PostgreSQL is up");
            return HealthCheckResponse.named("PostgreSQL")
                    .up()
                    .withData("PostgreSQL", "is up")
                    .withData("description", "PostgreSQL database is operational.")
                    .build();
            
        } else {
            LOGGER.error("PostgreSQL is down");
            return HealthCheckResponse.named("PostgreSQL")
                    .down()
                    .withData("PostgreSQL", "is down")
                    .withData("description", "PostgreSQL database is not reachable.")
                    .build();

        }
    }

    private boolean checkServiceHealth() {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            LOGGER.info("\n\n\nConexão postgres estabelecida\n\n\n");
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            LOGGER.error("PostgreSQL connection failed: " + e.getMessage());
            return false;
        }
    }
}
