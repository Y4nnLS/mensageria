package org.acme;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import io.quarkus.scheduler.Scheduled;

@Readiness // Define que esta classe faz parte do mecanismo de verificação de prontidão da aplicação
@ApplicationScoped // Define que a instância desta classe deve ser única durante o ciclo de vida da aplicação
public class PostgresHealthCheck implements HealthCheck {

    // URL de conexão com o banco de dados PostgreSQL
    String jdbcUrl = "jdbc:postgresql://postgres:5432/mensageria_DB";

    // Nome de usuário para conexão com o banco de dados
    String username = "admin";

    // Senha para conexão com o banco de dados
    String password = "123";

    // Intervalo de retry em milissegundos
    private static final int RETRY_INTERVAL_MS = 5000;
    // Número máximo de tentativas de reconexão
    private static final int MAX_RETRIES = 3;

    // Logger para registrar informações e erros
    private static final Logger LOGGER = Logger.getLogger(PostgresHealthCheck.class);

    /**
     * Método chamado para verificar a saúde do serviço PostgreSQL.
     * 
     * Este método é chamado quando o sistema precisa verificar se o serviço
     * de banco de dados PostgreSQL está operacional. Ele verifica se a conexão
     * com o banco de dados pode ser estabelecida com sucesso.
     * 
     * O método call() é chamado sempre que o endpoint de prontidão (geralmente `/q/health/ready` ou `/health/ready`) é acessado. 
     * Este endpoint é disponibilizado pelo servidor de aplicação para verificar se a aplicação está pronta para receber tráfego de produção.
     * 
     * @return HealthCheckResponse indicando o estado do serviço.
     */
    @Override
    public HealthCheckResponse call() {
        boolean isUp = checkServiceHealth(); // Verifica a saúde do serviço

        if (isUp) {
            return HealthCheckResponse.named("PostgreSQL")
                    .up() // Marca o serviço como "up" (ativo)
                    .withData("PostgreSQL", "is up")
                    .withData("description", "PostgreSQL database is operational.")
                    .build(); // Retorna resposta indicando que o PostgreSQL está ativo
        } else {
            return HealthCheckResponse.named("PostgreSQL")
                    .down() // Marca o serviço como "down" (inativo)
                    .withData("PostgreSQL", "is down")
                    .withData("description", "PostgreSQL database is not reachable.")
                    .build(); // Retorna resposta indicando que o PostgreSQL está inativo
        }
    }

    /**
     * Método agendado para verificar a saúde do PostgreSQL a cada 5 segundos.
     * 
     * Este método é executado periodicamente para garantir que o banco de dados
     * PostgreSQL está funcionando corretamente. Se o serviço estiver inativo,
     * tenta reconectar e reinicia o contêiner Docker do PostgreSQL.
     */
    @Scheduled(every = "10s") // Executa a cada 10 segundos
    public void scheduledHealthCheck() {
        boolean isUp = checkServiceHealth(); // Verifica a saúde do serviço

        if (isUp) {
            LOGGER.info("PostgreSQL is up");
            ServiceState.setPostgresActive(true); // Atualiza o estado do serviço
        } else {
            LOGGER.info("PostgreSQL is down");
            ServiceState.setPostgresActive(false); // Atualiza o estado do serviço
            handleServiceDown(); // Lida com a falha do serviço
            // Reiniciar container Docker do PostgreSQL quando o serviço está "down"
            DockerContainerManager.restartContainer("postgres");        
        }
    }

    /**
     * Verifica a saúde do banco de dados PostgreSQL tentando abrir uma conexão.
     * 
     * Este método tenta conectar-se ao banco de dados usando as credenciais e
     * URL fornecidos. Se a conexão for bem-sucedida e não estiver fechada, o
     * serviço é considerado operacional.
     * 
     * @return true se a conexão for bem-sucedida, false caso contrário.
     */
    private boolean checkServiceHealth() {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            return connection != null && !connection.isClosed(); // Verifica se a conexão está aberta
        } catch (SQLException e) {
            // adicionei a atualização do ServiceState aqui para tentar diminuir o delay de reconhecimento que o banco de dados caiu, para minimizar a perda de mensagens
            ServiceState.setPostgresActive(false); // Atualiza o estado do serviço
            return false; // Retorna false se ocorrer uma exceção ao tentar conectar
        }
    }

    /**
     * Lida com a situação em que o serviço PostgreSQL está inativo, tentando reconectar.
     * 
     * Este método tenta reconectar ao banco de dados várias vezes, com um intervalo
     * entre as tentativas. Se não for possível reconectar após o número máximo de
     * tentativas, uma mensagem de erro é registrada. Caso contrário, o estado do
     * serviço é atualizado para ativo.
     */
    private void handleServiceDown() {
        LOGGER.info("PostgreSQL está 'down'. Tentando reconectar...");

        boolean isPostgreSQLUp = false;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Thread.sleep(RETRY_INTERVAL_MS); // Aguarda antes da próxima tentativa

                if (checkServiceHealth()) {
                    isPostgreSQLUp = true; // Marca como bem-sucedido se a conexão for estabelecida
                    break; // Sai do loop se a conexão for bem-sucedida
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restaura o status de interrupção
                LOGGER.error("Interrupção durante o retry: " + e.getMessage());
            }
        }

        if (!isPostgreSQLUp) {
            LOGGER.error("Não foi possível reconectar ao PostgreSQL após " + MAX_RETRIES + " tentativas.");
            LOGGER.error("Aplicação está desativada."); // Informa que a aplicação está desativada
        } else {
            LOGGER.info("PostgreSQL voltou a ficar ativo.");
            ServiceState.setPostgresActive(true); // Atualiza o estado do serviço para ativo
        }
    }
}
