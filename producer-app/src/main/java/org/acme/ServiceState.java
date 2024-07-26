package org.acme;

public class ServiceState {

    private static volatile boolean active = true; // Estado global do serviço

    // Método para definir o estado do serviço
    public static void setActive(boolean isActive) {
        active = isActive;
    }

    // Método para verificar o estado do serviço
    public static boolean isActive() {
        return active;
    }
}
