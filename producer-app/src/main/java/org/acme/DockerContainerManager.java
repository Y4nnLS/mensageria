package org.acme;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DockerContainerManager {

    // Tentei reiniciar os containers utilizando uma biblioteca Docker e também um Script.bat porém nenhum funcionou corretamente.
    // por isso pensei em fazer programaticamente executando um comando como se eu estivesse rodando o comando `docker restart <nome_do_container>` no terminal
    // dessa forma está funcionando corretamente o reinicio dos containers

    /**
     * Reinicia um contêiner Docker especificado pelo nome.
     * 
     * Este método utiliza o comando "docker restart" para reiniciar o contêiner
     * e exibe a saída no console. Em caso de erro, exibe mensagens de erro
     * apropriadas.
     * 
     * @param containerName O nome do contêiner Docker que deve ser reiniciado.
     */
    public static void restartContainer(String containerName) {
        try {
            // Construindo o comando para reiniciar o container
            ProcessBuilder processBuilder = new ProcessBuilder("docker", "restart", containerName);
            // Utiliza ProcessBuilder para construir o comando de reinício do Docker. O comando executado é equivalente a docker restart <containerName> no terminal.
            
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
