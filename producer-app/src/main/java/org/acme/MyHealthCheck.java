// package org.acme;

// import org.eclipse.microprofile.health.HealthCheck;
// import org.eclipse.microprofile.health.HealthCheckResponse;
// import org.eclipse.microprofile.health.Readiness;
// import org.eclipse.microprofile.config.inject.ConfigProperty;
// import jakarta.enterprise.context.ApplicationScoped;
// import jakarta.jms.Connection;
// import jakarta.jms.ConnectionFactory;
// import jakarta.jms.JMSException;
// import jakarta.jms.Session;

// import org.apache.qpid.jms.JmsConnectionFactory;

// import org.jboss.logging.Logger;

// import io.quarkus.scheduler.Scheduled;

// /**
//  * MyHealthCheck é uma classe que implementa um health check de prontidão para verificar
//  * a disponibilidade do serviço ActiveMQ na aplicação.
//  * 
//  * Esta classe utiliza as anotações @Readiness e @ApplicationScoped para definir que
//  * representa um check de prontidão e que deve ser escopada ao ciclo de vida da aplicação.
//  */
// @Readiness // Define que esta classe é responsável por verificar a prontidão da aplicação
// @ApplicationScoped // Define que a instância desta classe deve ser única durante o ciclo de vida da aplicação
// public class MyHealthCheck implements HealthCheck {

//     // Propriedades configuráveis através de arquivos de configuração
//     @ConfigProperty(name = "mp.messaging.connector.smallrye-amqp.host")
//     String brokerHost;

//     @ConfigProperty(name = "mp.messaging.connector.smallrye-amqp.port")
//     int brokerPort;

//     @ConfigProperty(name = "mp.messaging.connector.smallrye-amqp.username")
//     String username;

//     @ConfigProperty(name = "mp.messaging.connector.smallrye-amqp.password")
//     String password;

//     // Configurações para o retry
//     private static final int RETRY_INTERVAL_MS = 5000; // Intervalo de retry em milissegundos
//     private static final int MAX_RETRIES = 6; // Número máximo de tentativas

//     private static final Logger LOGGER = Logger.getLogger(MyHealthCheck.class);

//     /**
//      * Executa o health check de prontidão para verificar se o ActiveMQ está acessível.
//      *
//      * Este método tenta se conectar ao ActiveMQ usando as configurações fornecidas.
//      * Se a conexão for bem-sucedida, o método retorna uma resposta "up"; caso contrário,
//      * retorna uma resposta "down".
//      * 
//      * O método call() é chamado sempre que o endpoint de prontidão (geralmente `/q/health/ready` ou `/health/ready`) é acessado. 
//      * Este endpoint é disponibilizado pelo servidor de aplicação para verificar se a aplicação está pronta para receber tráfego de produção.
//      *
//      * @return HealthCheckResponse indicando o estado de saúde do ActiveMQ.
//      */
//     @Override
//     public HealthCheckResponse call() {
//         // Verifica se o serviço está ativo e gera a resposta de health check
//         boolean isUp = checkServiceHealth();

//         if (isUp) {
//             ServiceState.setActiveMQActive(true);
//             return HealthCheckResponse.named("ActiveMQ")
//                     .up() // Marca o serviço como "up" (ativo)
//                     .withData("ActiveMQ", "is up")
//                     .withData("description", "ActiveMQ is operational.")
//                     .build();
//         } else {
//             ServiceState.setActiveMQActive(false);
//             handleServiceDown(); // Tenta reconectar se o serviço estiver "down"
//             return HealthCheckResponse.named("ActiveMQ")
//                     .down() // Marca o serviço como "down" (inativo)
//                     .withData("ActiveMQ", "is down")
//                     .withData("description", "ActiveMQ is not reachable.")
//                     .build();
//         }
//     }

//     /**
//      * Verificação de saúde agendada para executar a cada minuto.
//      * 
//      * Este método executa periodicamente para verificar se o ActiveMQ está acessível.
//      * Se o serviço estiver "down", ele tenta reconectar e reinicia os contêineres Docker
//      * relacionados, se necessário.
//      */
//     @Scheduled(every = "30s") // Agenda a verificação de saúde para executar a cada minuto
//     public void scheduledHealthCheck() {
//         // Verifica periodicamente se o serviço está ativo
//         boolean isUp = checkServiceHealth();

//         if (isUp) {
//             LOGGER.info("ActiveMQ is up");
//             ServiceState.setActiveMQActive(true);
//         } else {
//             LOGGER.info("ActiveMQ is down");
//             ServiceState.setActiveMQActive(false);
//             handleServiceDown(); // Tenta reconectar se o serviço estiver "down"
//             // Reiniciar container Docker do ActiveMQ e da aplicação produtora quando o serviço está "down"
//             DockerContainerManager.restartContainer("activemq");
//             DockerContainerManager.restartContainer("producer-app");
//         }
//     }

//     /**
//      * Verifica se o ActiveMQ está acessível tentando estabelecer uma conexão.
//      *
//      * Este método tenta se conectar ao broker ActiveMQ usando as configurações fornecidas
//      * e retorna true se a conexão for bem-sucedida ou false se houver uma falha.
//      *
//      * @return boolean indicando se o ActiveMQ está acessível.
//      */
//     private boolean checkServiceHealth() {
//         try {
//             // Constrói a URL do broker usando as propriedades configuradas
//             String brokerUrl = String.format("amqp://%s:%d", brokerHost, brokerPort);
//             ConnectionFactory connectionFactory = new JmsConnectionFactory(brokerUrl);

//             try (Connection connection = connectionFactory.createConnection(username, password)) {
//                 connection.start(); // Inicia a conexão
//                 try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
//                     return session != null;
//                 }
//             }
//         } catch (JMSException e) {
//             // adicionei a atualização do ServiceState aqui para tentar diminuir o delay de reconhecimento que o broker caiu, para minimizar a perda de mensagens
//             ServiceState.setActiveMQActive(false);
//             return false;
//         }
//     }

//     /**
//      * Manipula a situação em que o serviço ActiveMQ está "down".
//      *
//      * Este método tenta reconectar ao ActiveMQ várias vezes, com um intervalo de espera entre as tentativas.
//      * Se todas as tentativas falharem, ele loga a falha e indica que o serviço está inativo.
//      */
//     private void handleServiceDown() {
//         // Loga a informação de que o serviço está "down" e tenta reconectar
//         LOGGER.info("ActiveMQ está 'down'. Tentando reconectar...");

//         boolean isActiveMQUp = false;
//         for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
//             try {
//                 Thread.sleep(RETRY_INTERVAL_MS); // Espera antes da próxima tentativa

//                 if (checkServiceHealth()) {
//                     isActiveMQUp = true;
//                     break; // Sai do loop se a conexão for bem-sucedida
//                 }
//             } catch (InterruptedException e) {
//                 Thread.currentThread().interrupt(); // Restaura o status de interrupção
//                 LOGGER.error("Interrupção durante o retry: " + e.getMessage());
//             }
//         }

//         if (!isActiveMQUp) {
//             // Loga a falha após as tentativas de reconexão
//             LOGGER.error("Não foi possível reconectar ao ActiveMQ após " + MAX_RETRIES + " tentativas.");
//             LOGGER.error("Aplicação está desativada.");
//         } else {
//             // Loga a reconexão bem-sucedida e reativa o serviço
//             LOGGER.info("ActiveMQ voltou a ficar ativo.");
//             ServiceState.setActiveMQActive(true); // Reativa o serviço quando o ActiveMQ volta
//         }
//     }
// }

package org.acme;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.PostConstruct;
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

    private static final int RETRY_INTERVAL_MS = 10000;
    private static final int MAX_RETRIES = 6;

    private static final Logger LOGGER = Logger.getLogger(MyHealthCheck.class);

    private Connection connection;
    private Session session;

    @PostConstruct
    public void init() {
        LOGGER.info("iniciando a verificação de disponibilidade do ActiveMQ");
        // Inicia a verificação de disponibilidade do ActiveMQ
        checkAndEstablishConnection();
    }

    @Override
    public HealthCheckResponse call() {
        LOGGER.info("call foi chamado");
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

    @Scheduled(every = "30s")
    public void scheduledHealthCheck() {
        LOGGER.info("Fazendo verificação agendada");
        boolean isUp = checkServiceHealth();

        if (isUp) {
            LOGGER.info("ActiveMQ is up");
            ServiceState.setActiveMQActive(true);
        } else {
            LOGGER.info("ActiveMQ is down");
            ServiceState.setActiveMQActive(false);
            checkAndEstablishConnection();
            // handleServiceDown();
            DockerContainerManager.restartContainer("activemq");
            DockerContainerManager.restartContainer("producer-app");
        }
    }

    private void checkAndEstablishConnection() {
        LOGGER.info("Conectando no ActiveMQ...");

        // Primeiro, limpe qualquer conexão anterior
        closeConnection();
        
        boolean isActiveMQUp = false;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            LOGGER.info("iniciando tentativa " + attempt + " de conexão");
            if (session == null) {
                try {
                    LOGGER.info("sla1");
                    String brokerUrl = String.format("amqp://%s:%d", brokerHost, brokerPort);
                    LOGGER.info("sla2");
                    ConnectionFactory connectionFactory = new JmsConnectionFactory(brokerUrl);
                    LOGGER.info("sla3");
                    connection = connectionFactory.createConnection(username, password);
                    LOGGER.info("sla4");
                    connection.start();
                    LOGGER.info("sla5");
                    session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    LOGGER.info("ActiveMQ connection initialized successfully.");
                    isActiveMQUp = true;
                    LOGGER.info("sla6");
                    break;
                } catch (JMSException e) {
                    LOGGER.error("Failed to create ActiveMQ connection: " + e.getMessage());
                }
            } else {
                LOGGER.info("ActiveMQ not yet available. Retrying...");
                try {
                    Thread.sleep(RETRY_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.error("Interrupted while waiting to retry: " + e.getMessage());
                }
            }
        }

        if (!isActiveMQUp) {
            LOGGER.error("Unable to establish connection to ActiveMQ after " + MAX_RETRIES + " attempts.");
        } else {
            LOGGER.info("ActiveMQ voltou a ficar ativo.");
            ServiceState.setActiveMQActive(true);
        }
    }

    // private boolean checkServiceHealth() {
    // LOGGER.info("checando saúde do activeMQ");
    // LOGGER.info("\n\n\n" + session + "\n\n\n");
    // try {
    // if (session != null) {
    // return true; // Se a sessão estiver ativa, o serviço está saudável
    // } else {
    // return false; // Caso contrário, o serviço está "down"
    // }
    // } catch (Exception e) {
    // return false; // Se ocorrer uma exceção, considera o serviço como "down"
    // }
    // }
    private boolean checkServiceHealth() {
        LOGGER.info("Verificando a saúde do ActiveMQ");

        // Primeiro, verifica se a conexão e a sessão ainda são válidas
        if (session == null) {
            LOGGER.info("Sessão é null, conexão não está saudável.");
            return false;
        }

        try {
            // Tenta criar um consumidor temporário para verificar a saúde da sessão
            session.createTemporaryQueue();
            LOGGER.info("Sessão válida, ActiveMQ está operacional.");
            return true;
        } catch (JMSException e) {
            LOGGER.error("Falha ao verificar a saúde da sessão: " + e.getMessage());

            // Se houver um erro, define a sessão como null para indicar que não está mais
            // ativa
            closeConnection();
            return false;
        }
    }

    private void closeConnection() {
        try {
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (JMSException e) {
            LOGGER.error("Erro ao fechar a conexão ou sessão: " + e.getMessage());
        } finally {
            session = null;
            connection = null;
        }
    }

    private void handleServiceDown() {
        LOGGER.info("ActiveMQ está 'down'. Tentando reconectar...");

        boolean isActiveMQUp = false;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Thread.sleep(RETRY_INTERVAL_MS);

                if (checkServiceHealth()) {
                    isActiveMQUp = true;
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Interrupção durante o retry: " + e.getMessage());
            }
        }

        if (!isActiveMQUp) {
            LOGGER.error("Não foi possível reconectar ao ActiveMQ após " + MAX_RETRIES + " tentativas.");
            LOGGER.error("Aplicação está desativada.");
        } else {
            LOGGER.info("ActiveMQ voltou a ficar ativo.");
            ServiceState.setActiveMQActive(true);
        }
    }
}
