package org.acme;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;


@Readiness
@ApplicationScoped
public class PostgresHealthCheck implements HealthCheck {

    @Inject
    JmsConnectionManager jmsConnectionManager;

    private static final Logger LOGGER = Logger.getLogger(PostgresHealthCheck.class);

    @Override
    public HealthCheckResponse call() {
        boolean isUp = jmsConnectionManager.checkPostgreSQLHealth();

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
}
