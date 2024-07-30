package org.acme;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Session;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;

@Readiness
@ApplicationScoped
public class MyHealthCheck implements HealthCheck {


    @Inject
    JmsConnectionManager jmsConnectionManager;

    private static final int RETRY_INTERVAL_MS = 5000; // Intervalo de retry em milissegundos
    private static final int MAX_RETRIES = 3; // Número máximo de tentativas

    private static final Logger LOGGER = Logger.getLogger(MyHealthCheck.class);

    @Override
    public HealthCheckResponse call() {
        boolean isUp = checkServiceHealth();

        if (isUp) {
            ServiceState.setActive(true);
            return HealthCheckResponse.named("ActiveMQ")
                    .up()
                    .withData("ActiveMQ", "is up")
                    .withData("description", "ActiveMQ is operational.")
                    .build();
        } else {
            ServiceState.setActive(false);
            handleServiceDown();
            return HealthCheckResponse.named("ActiveMQ")
                    .down()
                    .withData("ActiveMQ", "is down")
                    .withData("description", "ActiveMQ is not reachable.")
                    .build();
        }
    }
    @Scheduled(every="5s") // Verifica a cada 5 segundos
    public void scheduledHealthCheck() {
        boolean isUp = checkServiceHealth();

        if (isUp) {
            LOGGER.info("ActiveMQ is up");
            ServiceState.setActive(true);
        } else {
            LOGGER.info("ActiveMQ is down");
            ServiceState.setActive(false);
            handleServiceDown();
        }
    }

    private boolean checkServiceHealth() {
        try {
            Connection connection = jmsConnectionManager.getConnection();
            try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                return session != null;
            } finally {
                connection.close(); // Fechar a conexão após o teste
            }
        } catch (JMSException e) {
            return false;
        }
    }

    private void handleServiceDown() {
        LOGGER.info("ActiveMQ está 'down'. Tentando reconectar...");

        boolean isActiveMQUp = false;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Thread.sleep(RETRY_INTERVAL_MS); // Espera antes da próxima tentativa

                if (checkServiceHealth()) {
                    isActiveMQUp = true;
                    break; // Sai do loop se a conexão for bem-sucedida
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restaura o status de interrupção
                LOGGER.error("Interrupção durante o retry: " + e.getMessage());
            }
        }

        if (!isActiveMQUp) {
            LOGGER.error("Não foi possível reconectar ao ActiveMQ após " + MAX_RETRIES + " tentativas.");
            LOGGER.error("Aplicação está desativada.");
        } else {
            LOGGER.info("ActiveMQ voltou a ficar ativo.");
            ServiceState.setActive(true); // Reativa o serviço quando o ActiveMQ volta
        }
    }
}
