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

@ApplicationScoped
public class EquipmentScheduler {

    private static final Logger LOGGER = Logger.getLogger(EquipmentScheduler.class);

    // URL do endpoint do Producer App para onde os dados serão enviados
    private static final String TARGET_URL = "http://producer-app:8083/api/api/data";

    // Lista para armazenar mensagens que falharam ao ser enviadas
    private final List<EquipmentData> failedMessages = new LinkedList<>();

    // Método agendado para ser executado a cada 1 minuto
    @Scheduled(every = "1m")
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

    // Método agendado para tentar reenviar mensagens falhadas a cada 1 minuto
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

    // Método para enviar dados em formato JSON para o endpoint especificado
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

    // Método para converter o objeto EquipmentData para uma string JSON
    private String convertToJson(EquipmentData data) {
        // Converte manualmente, mas pode-se usar bibliotecas como Jackson ou Gson
        return String.format("{\"value\":%d,\"timestamp\":\"%s\"}", data.getValue(), data.getTimestamp());
    }
}
