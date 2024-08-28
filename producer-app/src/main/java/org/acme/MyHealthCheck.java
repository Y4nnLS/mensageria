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

@Readiness // Define que esta classe é responsável por verificar a prontidão da aplicação
@ApplicationScoped // Define que a instância desta classe deve ser única durante o ciclo de vida da aplicação
public class MyHealthCheck implements HealthCheck {

    // Propriedades configuráveis através de arquivos de configuração
    @ConfigProperty(name = "mp.messaging.connector.smallrye-amqp.host")
    String brokerHost;

    @ConfigProperty(name = "mp.messaging.connector.smallrye-amqp.port")
    int brokerPort;

    @ConfigProperty(name = "mp.messaging.connector.smallrye-amqp.username")
    String username;

    @ConfigProperty(name = "mp.messaging.connector.smallrye-amqp.password")
    String password;

    // Configurações para o retry
    private static final int RETRY_INTERVAL_MS = 10000; // Intervalo de retry em milissegundos
    private static final int MAX_RETRIES = 3; // Número máximo de tentativas

    private static final Logger LOGGER = Logger.getLogger(MyHealthCheck.class);

    @Override
    public HealthCheckResponse call() {
        // Verifica se o serviço está ativo e gera a resposta de health check
        boolean isUp = checkServiceHealth();

        if (isUp) {
            ServiceState.setActiveMQActive(true);
            return HealthCheckResponse.named("ActiveMQ")
                    .up() // Marca o serviço como "up" (ativo)
                    .withData("ActiveMQ", "is up")
                    .withData("description", "ActiveMQ is operational.")
                    .build();
        } else {
            ServiceState.setActiveMQActive(false);
            handleServiceDown(); // Tenta reconectar se o serviço estiver "down"
            return HealthCheckResponse.named("ActiveMQ")
                    .down() // Marca o serviço como "down" (inativo)
                    .withData("ActiveMQ", "is down")
                    .withData("description", "ActiveMQ is not reachable.")
                    .build();
        }
    }

    @Scheduled(every = "30s") // Agenda a verificação de saúde para executar a cada 10 segundos
    public void scheduledHealthCheck() {
        // Verifica periodicamente se o serviço está ativo
        boolean isUp = checkServiceHealth();

        if (isUp) {
            LOGGER.info("ActiveMQ is up");
            ServiceState.setActiveMQActive(true);
        } else {
            LOGGER.info("ActiveMQ is down");
            ServiceState.setActiveMQActive(false);
            handleServiceDown(); // Tenta reconectar se o serviço estiver "down"
            // Reiniciar container Docker do PostgreSQL quando o serviço está "down"
            DockerContainerManager.restartContainer("activemq");
        }
    }

    private boolean checkServiceHealth() {
        try {
            // Constrói a URL do broker usando as propriedades configuradas
            String brokerUrl = String.format("amqp://%s:%d", brokerHost, brokerPort);
            ConnectionFactory connectionFactory = new JmsConnectionFactory(brokerUrl);

            try (Connection connection = connectionFactory.createConnection(username, password)) {
                connection.start(); // Inicia a conexão
                try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                    // Verifica se a sessão foi criada corretamente
                    return session != null;
                }
            }
        } catch (JMSException e) {
            // Captura exceções de JMS e retorna false se a conexão falhar
            return false;
        }
    }

    private void handleServiceDown() {
        // Loga a informação de que o serviço está "down" e tenta reconectar
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
            // Loga a falha após as tentativas de reconexão
            LOGGER.error("Não foi possível reconectar ao ActiveMQ após " + MAX_RETRIES + " tentativas.");
            LOGGER.error("Aplicação está desativada.");
        } else {
            // Loga a reconexão bem-sucedida e reativa o serviço
            LOGGER.info("ActiveMQ voltou a ficar ativo.");
            ServiceState.setActiveMQActive(true); // Reativa o serviço quando o ActiveMQ volta
        }
    }
}
