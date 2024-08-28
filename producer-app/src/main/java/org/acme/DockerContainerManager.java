package org.acme;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DockerContainerManager {

    public static void restartContainer(String containerName) {
        try {
            // Construindo o comando para reiniciar o container
            ProcessBuilder processBuilder = new ProcessBuilder("docker", "restart", containerName);
            
            // Iniciando o processo
            Process process = processBuilder.start();
            
            // Lendo a saída do comando
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line); // Exibindo a saída no console
                }
            }

            // Verificando o código de saída para garantir que o comando foi bem-sucedido
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Container " + containerName + " reiniciado com sucesso.");
            } else {
                System.err.println("Erro ao reiniciar o container " + containerName + ". Código de saída: " + exitCode);
            }

        } catch (Exception e) {
            System.err.println("Erro ao tentar reiniciar o container: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
