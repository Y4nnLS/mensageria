package org.acme;

public class ServiceState {

    private static volatile boolean postgresActive = true; // Estado do PostgreSQL
    private static volatile boolean activeMQActive = true; // Estado do ActiveMQ

    // Método para definir o estado do PostgreSQL
    public static void setPostgresActive(boolean isActive) {
        postgresActive = isActive;
    }

    // Método para verificar o estado do PostgreSQL
    public static boolean isPostgresActive() {
        return postgresActive;
    }

    // Método para definir o estado do ActiveMQ
    public static void setActiveMQActive(boolean isActive) {
        activeMQActive = isActive;
    }

    // Método para verificar o estado do ActiveMQ
    public static boolean isActiveMQActive() {
        return activeMQActive;
    }

    // Método para verificar se ambos os serviços estão ativos
    public static boolean isApplicationActive() {
        return postgresActive && activeMQActive;
    }
}
