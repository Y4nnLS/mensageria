package org.acme;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Session;

import org.apache.qpid.jms.JmsConnectionFactory;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;

@Readiness
@ApplicationScoped
public class MyHealthCheck implements HealthCheck {

    @ConfigProperty(name = "mp.messaging.connector.smallrye-amqp.host")
    String brokerHost;

    @ConfigProperty(name = "mp.messaging.connector.smallrye-amqp.port")
    int brokerPort;

    @ConfigProperty(name = "mp.messaging.connector.smallrye-amqp.username")
    String username;

    @ConfigProperty(name = "mp.messaging.connector.smallrye-amqp.password")
    String password;

    private static final int RETRY_INTERVAL_MS = 5000; // Intervalo de retry em milissegundos
    private static final int MAX_RETRIES = 3; // Número máximo de tentativas

    private static final Logger LOGGER = Logger.getLogger(MyHealthCheck.class);

    @Override
    public HealthCheckResponse call() {
        boolean isUp = checkServiceHealth();

        if (isUp) {
            ServiceState.setActiveMQActive(true);
            return HealthCheckResponse.named("ActiveMQ")
                    .up()
                    .withData("ActiveMQ", "is up")
                    .withData("description", "ActiveMQ is operational.")
                    .build();
        } else {
            ServiceState.setActiveMQActive(false);
            handleServiceDown();
            return HealthCheckResponse.named("ActiveMQ")
                    .down()
                    .withData("ActiveMQ", "is down")
                    .withData("description", "ActiveMQ is not reachable.")
                    .build();
        }
    }

    @Scheduled(every = "5s") // Verifica a cada 5 segundos
    public void scheduledHealthCheck() {
        boolean isUp = checkServiceHealth();

        if (isUp) {
            LOGGER.info("ActiveMQ is up");
            ServiceState.setActiveMQActive(true);
        } else {
            LOGGER.info("ActiveMQ is down");
            ServiceState.setActiveMQActive(false);
            handleServiceDown();
        }
    }

    private boolean checkServiceHealth() {
        try {
            String brokerUrl = String.format("amqp://%s:%d", brokerHost, brokerPort);
            ConnectionFactory connectionFactory = new JmsConnectionFactory(brokerUrl);

            try (Connection connection = connectionFactory.createConnection(username, password)) {
                connection.start();
                try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                    return session != null;
                }
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
            ServiceState.setActiveMQActive(true); // Reativa o serviço quando o ActiveMQ volta
        }
    }
}
