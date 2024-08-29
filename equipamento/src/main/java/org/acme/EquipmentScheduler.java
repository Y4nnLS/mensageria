package org.acme;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * A classe EquipmentScheduler é responsável por agendar tarefas de envio de dados
 * e reenvio de mensagens falhadas. Utiliza o Quarkus Scheduler para executar tarefas
 * periodicamente.
 */
@ApplicationScoped
public class EquipmentScheduler {

    private static final Logger LOGGER = Logger.getLogger(EquipmentScheduler.class);

    // URL do endpoint do Producer App para onde os dados serão enviados
    private static final String TARGET_URL = "http://producer-app:8083/api/api/data";

    // Lista para armazenar mensagens que falharam ao ser enviadas
    private final List<EquipmentData> failedMessages = new LinkedList<>();

    // Contador para gerar valores em ordem crescente
    // isso é para testes para ver se nenhuma mensagem está sendo perdida quando o activeMQ ou o banco de dados cai
    private int currentValue = 0;

    /**
     * Método agendado para ser executado a cada 1 minuto. Gera um valor crescente,
     * cria um objeto EquipmentData e tenta enviar os dados para o endpoint especificado.
     */
    @Scheduled(every = "1m")
    public void sendSequentialValue() {
        // Gera um valor crescente
        int value = currentValue++;
        
        // Obtém a data e hora atual formatada
        LocalDateTime now = LocalDateTime.now().withSecond(0);
        String formattedDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        // Cria um objeto EquipmentData com o valor gerado e a data/hora formatada
        EquipmentData data = new EquipmentData(value, formattedDate);
        LOGGER.infof("Enviando dados: %s", data);

        try {
            // Envia os dados para o endpoint do Producer App
            sendJsonData(TARGET_URL, data);
            LOGGER.infof("Dados enviados com sucesso: %s", data);
        } catch (Exception e) {
            // Se ocorrer uma exceção, adiciona os dados à lista de mensagens falhadas
            LOGGER.errorf("Erro ao enviar dados: %s", e.getMessage());
            failedMessages.add(data);
        }
    }
    /**
     * Método agendado para ser executado a cada 1 minuto. Gera um valor aleatório,
     * cria um objeto EquipmentData e tenta enviar os dados para o endpoint especificado.
     */
    // @Scheduled(every = "1m")
    public void sendRandomValue() {
        // Gera um valor aleatório
        Random random = new Random();
        int value = random.nextInt(100);

        // Obtém a data e hora atual formatada
        LocalDateTime now = LocalDateTime.now().withSecond(0);
        String formattedDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        // Cria um objeto EquipmentData com o valor gerado e a data/hora formatada
        EquipmentData data = new EquipmentData(value, formattedDate);
        LOGGER.infof("Enviando dados: %s", data);

        try {
            // Envia os dados para o endpoint do Producer App
            sendJsonData(TARGET_URL, data);
            LOGGER.infof("Dados enviados com sucesso: %s", data);
        } catch (Exception e) {
            // Se ocorrer uma exceção, adiciona os dados à lista de mensagens falhadas
            LOGGER.errorf("Erro ao enviar dados: %s", e.getMessage());
            failedMessages.add(data);
        }
    }

    /**
     * Método agendado para tentar reenviar mensagens falhadas a cada 1 minuto.
     * Reenvia as mensagens que não foram enviadas com sucesso anteriormente.
     */
    @Scheduled(every = "1m")
    public void retryFailedMessages() {
        LOGGER.info("Tentando reenviar mensagens falhadas...");
        List<EquipmentData> messagesToRetry = new LinkedList<>(failedMessages);
        failedMessages.clear();

        for (EquipmentData data : messagesToRetry) {
            try {
                // Tenta reenviar as mensagens falhadas
                sendJsonData(TARGET_URL, data);
                LOGGER.infof("Mensagem reenviada com sucesso: %s", data);
            } catch (Exception e) {
                // Se ocorrer uma exceção, adiciona a mensagem de volta à lista de falhas
                LOGGER.errorf("Erro ao reenviar mensagem: %s", e.getMessage());
                failedMessages.add(data);
            }
        }
    }

    /**
     * Envia os dados em formato JSON para o endpoint especificado.
     *
     * @param targetUrl URL do endpoint para onde os dados serão enviados
     * @param data Objeto EquipmentData contendo os dados a serem enviados
     * @throws Exception Se ocorrer um erro ao enviar os dados
     */
    private void sendJsonData(String targetUrl, EquipmentData data) throws Exception {
        URI uri = URI.create(targetUrl);
        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setDoOutput(true);

        // Converte o objeto EquipmentData para uma string JSON
        String jsonInputString = convertToJson(data);

        // Envia o JSON no corpo da requisição
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        LOGGER.infof("Código de resposta: %d", responseCode);

        // Lança uma exceção se o código de resposta não for HTTP 200 OK
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Falha ao enviar dados: Código de resposta " + responseCode);
        }
    }

    /**
     * Converte o objeto EquipmentData para uma string JSON.
     *
     * @param data Objeto EquipmentData a ser convertido
     * @return A representação JSON do objeto EquipmentData
     */
    private String convertToJson(EquipmentData data) {
        // Converte manualmente, mas pode-se usar bibliotecas como Jackson
        return String.format("{\"value\":%d,\"timestamp\":\"%s\"}", data.getValue(), data.getTimestamp());
    }
}
