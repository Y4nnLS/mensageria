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

    private static final String TARGET_URL = "http://producer-app:8083/api/api/data";
    // private static final int RETRY_INTERVAL_MS = 60000; // 1 minute

    private final List<EquipmentData> failedMessages = new LinkedList<>();

    @Scheduled(every = "1m")
    public void sendRandomValue() {
        Random random = new Random();
        int value = random.nextInt(100);
        LocalDateTime now = LocalDateTime.now().withSecond(0);
        String formattedDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        EquipmentData data = new EquipmentData(value, formattedDate);
        LOGGER.infof("Enviando dados: %s", data);

        try {
            sendJsonData(TARGET_URL, data);
            LOGGER.infof("Dados enviados com sucesso: %s", data);
        } catch (Exception e) {
            LOGGER.errorf("Erro ao enviar dados: %s", e.getMessage());
            failedMessages.add(data);
        }
    }

    @Scheduled(every = "1m")
    public void retryFailedMessages() {
        LOGGER.info("Tentando reenviar mensagens falhadas...");
        List<EquipmentData> messagesToRetry = new LinkedList<>(failedMessages);
        failedMessages.clear();

        for (EquipmentData data : messagesToRetry) {
            try {
                sendJsonData(TARGET_URL, data);
                LOGGER.infof("Mensagem reenviada com sucesso: %s", data);
            } catch (Exception e) {
                LOGGER.errorf("Erro ao reenviar mensagem: %s", e.getMessage());
                failedMessages.add(data);
            }
        }
    }

    private void sendJsonData(String targetUrl, EquipmentData data) throws Exception {
        URI uri = URI.create(targetUrl);
        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setDoOutput(true);

        String jsonInputString = convertToJson(data);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        LOGGER.infof("Código de resposta: %d", responseCode);

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Falha ao enviar dados: Código de resposta " + responseCode);
        }
    }

    private String convertToJson(EquipmentData data) {
        // Aqui estamos usando manualmente, mas você pode usar Jackson ou Gson se preferir
        return String.format("{\"value\":%d,\"timestamp\":\"%s\"}", data.getValue(), data.getTimestamp());
    }
}
