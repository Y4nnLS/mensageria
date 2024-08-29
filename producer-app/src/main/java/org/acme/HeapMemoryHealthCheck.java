package org.acme;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

@Readiness // Indica que esta classe faz parte do mecanismo de verificação de prontidão da
           // aplicação.
@ApplicationScoped // Define que a instância desta classe deve ser única durante o ciclo de vida da
                   // aplicação.
public class HeapMemoryHealthCheck implements HealthCheck {

    // Limiares de uso de heap para disparar avisos e estados críticos.
    private static final double WARNING_THRESHOLD = 80.00; // % de uso de heap para aviso.
    private static final double CRITICAL_THRESHOLD = 90.00; // % de uso de heap para estado crítico.

    private static final Logger LOGGER = Logger.getLogger(HeapMemoryHealthCheck.class);

    // Variáveis para armazenar informações sobre o uso de memória.
    private long usedMemory;
    private long maxMemory;
    private String formattedUsedMemoryPercentage;
    private int status;
    private volatile boolean shouldShutdown = false; // Indica se a aplicação deve ser desligada.
    private volatile boolean simulationActive = false; // Controle para simulação de erro de memória.

    /**
     * Ativa ou desativa a simulação de erro de memória.
     *
     * @param active Define se a simulação de erro de memória deve ser ativada.
     */
    public void setSimulationActive(boolean active) {
        simulationActive = active;
    }

    /**
     * Sinaliza que a aplicação deve ser desligada.
     */
    public void triggerShutdown() {
        shouldShutdown = true;
    }

    /**
     * Realiza o desligamento da aplicação se o uso de memória for crítico.
     * 
     * Este método é chamado após verificar que o uso de memória excede o limite
     * crítico.
     * O desligamento é feito iniciando uma nova thread que chama System.exit(1), o
     * que
     * resulta no término imediato da JVM com um código de erro.
     * 
     * É importante garantir que este método só seja chamado em condições críticas,
     * pois forçará o término da aplicação.
     */
    private void gracefulShutdown() {
        if (shouldShutdown) {
            new Thread(() -> {
                LOGGER.info("Shutting down application due to critical memory usage.");
                System.exit(1); // Encerra a aplicação com código de erro 1.
            }).start();
        }
    }

    /**
     * Verifica o uso de memória heap e retorna um objeto HealthCheckResponse
     * com base no status da memória.
     * 
     * É invocada automaticamente pelo Quarkus como parte do ciclo de verificação de
     * saúde, especialmente em endpoints de prontidão (readiness) para verificar se
     * a aplicação está pronta para receber tráfego.
     * 
     * @return HealthCheckResponse indicando o estado de saúde atual da memória
     *         heap.
     */
    @Override
    public HealthCheckResponse call() {
        try {
            // Verifica o uso de memória heap e atualiza o status.
            checkHeapMemoryUsage();

            // Retorna uma resposta de saúde com base no status de memória.
            if (status == 1) {
                return HealthCheckResponse.named("HeapMemory")
                        .down() // Marca o serviço como "down" devido ao uso crítico de memória.
                        .withData("usedMemory", usedMemory)
                        .withData("maxMemory", maxMemory)
                        .withData("usedMemoryPercentage", formattedUsedMemoryPercentage + "%")
                        .withData("status", "CRITICAL")
                        .withData("description", "Heap memory usage is critically high.")
                        .build();
            } else if (status == 2) {
                return HealthCheckResponse.named("HeapMemory")
                        .up() // Marca o serviço como "up" com aviso de uso de memória alto.
                        .withData("usedMemory", usedMemory)
                        .withData("maxMemory", maxMemory)
                        .withData("usedMemoryPercentage", formattedUsedMemoryPercentage + "%")
                        .withData("status", "WARNING")
                        .withData("description", "Heap memory usage is high.")
                        .build();
            } else {
                return HealthCheckResponse.named("HeapMemory")
                        .up() // Marca o serviço como "up" com uso de memória dentro dos limites normais.
                        .withData("usedMemory", usedMemory)
                        .withData("maxMemory", maxMemory)
                        .withData("usedMemoryPercentage", formattedUsedMemoryPercentage + "%")
                        .withData("status", "OK")
                        .withData("description", "Heap memory usage is within normal limits.")
                        .build();
            }
        } catch (OutOfMemoryError e) {
            // Captura erros de memória e retorna uma resposta de saúde indicando erro.
            LOGGER.error("Caught OutOfMemoryError: " + e.getMessage());
            return HealthCheckResponse.named("HeapMemory")
                    .down()
                    .withData("status", "ERROR")
                    .withData("description", "Heap memory usage has caused an OutOfMemoryError.")
                    .build();
        }
    }

    /**
     * Verificação agendada do uso de memória heap. Executa a cada 5 segundos.
     * Este método verifica regularmente o uso de memória e executa ações
     * apropriadas
     * como notificações e desligamento.
     */
    @Scheduled(every = "1m")
    public void scheduledHealthCheck() {
        try {
            // Verifica o uso de memória heap e atualiza o status.
            checkHeapMemoryUsage();

            // Simula erro de memória se a simulação estiver ativa.
            if (simulationActive) {
                simulateOutOfMemoryError();
            }

            // printa e notifica sobre o status de memória com base no uso atual.
            if (status == 1) {
                LOGGER.info("HEAP CRITICAL");
                notifyUser("CRITICAL", usedMemory, maxMemory, formattedUsedMemoryPercentage,
                        "Heap memory usage is critically high.");
            } else if (status == 2) {
                LOGGER.info("HEAP WARNING");
                notifyUser("WARNING", usedMemory, maxMemory, formattedUsedMemoryPercentage,
                        "Heap memory usage is high.");
            } else {
                LOGGER.info("HEAP OK");
                notifyUser("OK", usedMemory, maxMemory, formattedUsedMemoryPercentage,
                        "Heap memory usage is within normal limits.");
            }
        } catch (OutOfMemoryError e) {
            // Captura erros de memória durante a verificação agendada e tenta desligar a
            // aplicação.
            LOGGER.error("Caught OutOfMemoryError during scheduled health check: " + e.getMessage());
            triggerShutdown();
            notifyUser("ERROR", 0, 0, "N/A",
                    "Heap memory usage has caused an OutOfMemoryError during scheduled health check.");
        }
        gracefulShutdown(); // Tenta realizar o desligamento se necessário.
    }

    /**
     * Verifica o uso de memória heap e define o status de acordo com os limiares.
     * Este método é usado para determinar se o uso de memória é normal, está em
     * aviso
     * ou é crítico.
     *
     * @return Um inteiro representando o status da memória:
     *         0 para OK, 1 para CRITICAL, 2 para WARNING.
     */
    public int checkHeapMemoryUsage() {
        // Obtenção do bean de gerenciamento de memória da JVM.
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        maxMemory = heapMemoryUsage.getMax();
        usedMemory = heapMemoryUsage.getUsed();

        double usedMemoryPercentage = (double) usedMemory / maxMemory * 100;
        formattedUsedMemoryPercentage = String.format("%.2f", usedMemoryPercentage);

        // Define o status com base no uso de memória.
        if (usedMemoryPercentage >= CRITICAL_THRESHOLD) {
            notifyUser("CRITICAL", usedMemory, maxMemory, formattedUsedMemoryPercentage,
                    "Heap memory usage is critically high.");
            status = 1;
            triggerShutdown(); // Aciona o desligamento se o uso de memória for crítico.
        } else if (usedMemoryPercentage >= WARNING_THRESHOLD) {
            notifyUser("WARNING", usedMemory, maxMemory, formattedUsedMemoryPercentage, "Heap memory usage is high.");
            status = 2;
        } else {
            notifyUser("OK", usedMemory, maxMemory, formattedUsedMemoryPercentage,
                    "Heap memory usage is within normal limits.");
            status = 0;
        }

        return status;
    }

    /**
     * Simula um erro de memória, preenchendo o heap até que ocorra um
     * OutOfMemoryError.
     * Usado para testar o comportamento da aplicação sob condições de memória
     * crítica.
     */
    private void simulateOutOfMemoryError() {
        if (!simulationActive) {
            return; // Se a simulação não estiver ativa, não faz nada.
        }

        List<byte[]> memoryHog = new ArrayList<>();
        int incrementSize = 1024 * 1024; // Tamanho do incremento em bytes (1 MB).
        int delay = 1000; // Delay entre as alocações em milissegundos.

        try {
            while (simulationActive) {
                // Adiciona chunks de memória para consumir heap.
                byte[] memoryChunk = new byte[incrementSize];
                memoryHog.add(memoryChunk);
                Thread.sleep(delay); // Pausa antes da próxima alocação.
            }
        } catch (OutOfMemoryError e) {
            throw e; // Repassa o erro de memória para ser tratado.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restaura o status de interrupção.
            triggerShutdown();
            LOGGER.error("Thread interrupted during memory simulation: " + e.getMessage());
        }
    }

    /**
     * Notifica sobre o status do uso de memória, printando informações detalhadas.
     *
     * @param status                        O status de memória (OK, WARNING,
     *                                      CRITICAL, ERROR).
     * @param usedMemory                    Memória usada em bytes.
     * @param maxMemory                     Memória máxima em bytes.
     * @param formattedUsedMemoryPercentage Porcentagem formatada da memória usada.
     * @param description                   Descrição adicional sobre o status.
     */
    private void notifyUser(String status, long usedMemory, long maxMemory, String formattedUsedMemoryPercentage,
            String description) {
        LOGGER.info("\n\n\n\n\nHeap Memory Status: " + status +
                "\nUsed: " + usedMemory +
                "\nMax: " + maxMemory +
                "\nUsed%: " + formattedUsedMemoryPercentage +
                "\nDescription: " + description + "\n\n\n\n\n");
    }
}
