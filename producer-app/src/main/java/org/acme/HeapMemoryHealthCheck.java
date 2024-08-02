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

@Readiness
@ApplicationScoped
public class HeapMemoryHealthCheck implements HealthCheck {

    private static final double WARNING_THRESHOLD = 80.00; // % de uso de heap para warning
    private static final double CRITICAL_THRESHOLD = 90.00; // % de uso de heap para critical

    private static final Logger LOGGER = Logger.getLogger(HeapMemoryHealthCheck.class);

    private long usedMemory;
    private long maxMemory;
    private String formattedUsedMemoryPercentage;
    private int status;
    private volatile boolean shouldShutdown = false;
    private volatile boolean simulationActive = false;

    public void setSimulationActive(boolean active) {
        simulationActive = active;

    }

    public void triggerShutdown() {
        shouldShutdown = true;
    }

    private void gracefulShutdown() {
        if (shouldShutdown) {
            new Thread(() -> {
                LOGGER.info("Shutting down application due to critical memory usage.");
                System.exit(1);
            }).start();
        }
    }

    @Override
    public HealthCheckResponse call() {
        try {
            checkHeapMemoryUsage();

            if (status == 1) {
                return HealthCheckResponse.named("HeapMemory")
                        .down()
                        .withData("usedMemory", usedMemory)
                        .withData("maxMemory", maxMemory)
                        .withData("usedMemoryPercentage", formattedUsedMemoryPercentage + "%")
                        .withData("status", "CRITICAL")
                        .withData("description", "Heap memory usage is critically high.")
                        .build();
            } else if (status == 2) {
                return HealthCheckResponse.named("HeapMemory")
                        .up()
                        .withData("usedMemory", usedMemory)
                        .withData("maxMemory", maxMemory)
                        .withData("usedMemoryPercentage", formattedUsedMemoryPercentage + "%")
                        .withData("status", "WARNING")
                        .withData("description", "Heap memory usage is high.")
                        .build();
            } else {
                return HealthCheckResponse.named("HeapMemory")
                        .up()
                        .withData("usedMemory", usedMemory)
                        .withData("maxMemory", maxMemory)
                        .withData("usedMemoryPercentage", formattedUsedMemoryPercentage + "%")
                        .withData("status", "OK")
                        .withData("description", "Heap memory usage is within normal limits.")
                        .build();
            }
        } catch (OutOfMemoryError e) {
            LOGGER.error("Caught OutOfMemoryError: " + e.getMessage());
            return HealthCheckResponse.named("HeapMemory")
                    .down()
                    .withData("status", "ERROR")
                    .withData("description", "Heap memory usage has caused an OutOfMemoryError.")
                    .build();
        }
    }

    @Scheduled(every = "5s")
    public void scheduledHealthCheck() {
        try {
            checkHeapMemoryUsage();
            
            if (simulationActive) {
                simulateOutOfMemoryError();
            }

            if (status == 1) {
                LOGGER.info("HEAP CRITICAL");
                triggerShutdown();
                notifyUser("CRITICAL", usedMemory, maxMemory, formattedUsedMemoryPercentage, "Heap memory usage is critically high.");
            } else if (status == 2) {
                LOGGER.info("HEAP WARNING");
                notifyUser("WARNING", usedMemory, maxMemory, formattedUsedMemoryPercentage, "Heap memory usage is high.");
            } else {
                LOGGER.info("HEAP OK");
                notifyUser("OK", usedMemory, maxMemory, formattedUsedMemoryPercentage, "Heap memory usage is within normal limits.");
            }
        } catch (OutOfMemoryError e) {
            LOGGER.error("Caught OutOfMemoryError during scheduled health check: " + e.getMessage());
            triggerShutdown();
            notifyUser("ERROR", 0, 0, "N/A", "Heap memory usage has caused an OutOfMemoryError during scheduled health check.");
        }
        gracefulShutdown();
    }

    public int checkHeapMemoryUsage() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        maxMemory = heapMemoryUsage.getMax();
        usedMemory = heapMemoryUsage.getUsed();

        double usedMemoryPercentage = (double) usedMemory / maxMemory * 100;
        formattedUsedMemoryPercentage = String.format("%.2f", usedMemoryPercentage);

        if (usedMemoryPercentage >= CRITICAL_THRESHOLD) {
            notifyUser("CRITICAL", usedMemory, maxMemory, formattedUsedMemoryPercentage, "Heap memory usage is critically high.");
            status = 1;
        } else if (usedMemoryPercentage >= WARNING_THRESHOLD) {
            notifyUser("WARNING", usedMemory, maxMemory, formattedUsedMemoryPercentage, "Heap memory usage is high.");
            status = 2;
        } else {
            notifyUser("OK", usedMemory, maxMemory, formattedUsedMemoryPercentage, "Heap memory usage is within normal limits.");
            status = 0;
        }

        return status;
    }

    private void simulateOutOfMemoryError() {
        if (!simulationActive) {
            return;
        }

        List<byte[]> memoryHog = new ArrayList<>();
        int incrementSize = 1024 * 1024;
        int delay = 1000;
        
        try {
            while (simulationActive) {
                byte[] memoryChunk = new byte[incrementSize];
                memoryHog.add(memoryChunk);
                Thread.sleep(delay);
            }
        } catch (OutOfMemoryError e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Thread interrupted during memory simulation: " + e.getMessage());
        }
    }

    private void notifyUser(String status, long usedMemory, long maxMemory, String formattedUsedMemoryPercentage, String description) {
        LOGGER.info("\n\n\n\n\nHeap Memory Status: "+ status + "\nUsed: " +usedMemory + "\nMax: " + maxMemory +  "\nUsed%%: "+ formattedUsedMemoryPercentage+ "\nDescription: "+ description+"\n\n\n\n\n");
    }
}
