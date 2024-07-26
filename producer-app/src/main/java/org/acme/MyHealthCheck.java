package org.acme;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.apache.qpid.jms.JmsConnectionFactory;

@Readiness
@ApplicationScoped
public class MyHealthCheck implements HealthCheck {

    private static final String BROKER_URL = "amqp://activemq:5672"; // Endereço do ActiveMQ no Docker
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    private static final int RETRY_INTERVAL_MS = 5000; // Intervalo de retry em milissegundos
    private static final int MAX_RETRIES = 12; // Número máximo de tentativas

    @Override
    public HealthCheckResponse call() {
        boolean isUp = checkServiceHealth();

        if (isUp) {
            ServiceState.setActive(true);
            return HealthCheckResponse.up("ActiveMQ is up");
        } else {
            ServiceState.setActive(false);
            handleServiceDown();
            return HealthCheckResponse.down("ActiveMQ is down");
        }
    }

    private boolean checkServiceHealth() {
        try {
            ConnectionFactory connectionFactory = new JmsConnectionFactory(BROKER_URL);

            try (Connection connection = connectionFactory.createConnection(USERNAME, PASSWORD)) {
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
        System.out.println("ActiveMQ está 'down'. Tentando reconectar...");

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
                System.out.println("Interrupção durante o retry: " + e.getMessage());
            }
        }

        if (!isActiveMQUp) {
            System.out.println("Não foi possível reconectar ao ActiveMQ após " + MAX_RETRIES + " tentativas.");
            System.out.println("Aplicação está desativada.");
        } else {
            System.out.println("ActiveMQ voltou a ficar ativo.");
        }
    }
}
