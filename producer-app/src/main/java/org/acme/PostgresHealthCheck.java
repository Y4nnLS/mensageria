package org.acme;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
// import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
// import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;
// import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;

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

    private static final int RETRY_INTERVAL_MS = 2000; // Intervalo de retry em milissegundos
    private static final int MAX_RETRIES = 3; // Número máximo de tentativas

    private static final Logger LOGGER = Logger.getLogger(PostgresHealthCheck.class);

    @Override
    public HealthCheckResponse call() {
        boolean isUp = checkServiceHealth();

        if (isUp) {
            return HealthCheckResponse.named("PostgreSQL")
                    .up()
                    .withData("PostgreSQL", "is up")
                    .withData("description", "PostgreSQL database is operational.")
                    .build();

        } else {
            return HealthCheckResponse.named("PostgreSQL")
                    .down()
                    .withData("PostgreSQL", "is down")
                    .withData("description", "PostgreSQL database is not reachable.")
                    .build();

        }
    }

    @Scheduled(every = "6s") // Verifica a cada 5 segundos
    public void scheduledHealthCheck() {
        boolean isUp = checkServiceHealth();

        if (isUp) {
            LOGGER.info("PostgreSQL is up");
            ServiceState.setPostgresActive(true);
        } else {
            LOGGER.info("PostgreSQL is down");
            ServiceState.setPostgresActive(false);
            handleServiceDown();
        }
    }

    private boolean checkServiceHealth() {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    private void handleServiceDown() {
        LOGGER.info("PostgreSQL está 'down'. Tentando reconectar...");

        boolean isPostgreSQLUp = false;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Thread.sleep(RETRY_INTERVAL_MS); // Espera antes da próxima tentativa

                if (checkServiceHealth()) {
                    isPostgreSQLUp = true;
                    break; // Sai do loop se a conexão for bem-sucedida
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restaura o status de interrupção
                LOGGER.error("Interrupção durante o retry: " + e.getMessage());
            }
        }

        if (!isPostgreSQLUp) {
            LOGGER.error("Não foi possível reconectar ao PostgreSQL após " + MAX_RETRIES + " tentativas.");
            LOGGER.error("Aplicação está desativada.");
        } else {
            LOGGER.info("PostgreSQL voltou a ficar ativo.");
            ServiceState.setPostgresActive(true); // Reativa o serviço quando o PostgreSQL volta
        }
    }
}