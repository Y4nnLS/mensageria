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
import java.util.Random;

@ApplicationScoped
public class EquipmentScheduler {

    private static final Logger LOGGER = Logger.getLogger(EquipmentScheduler.class);

    @Scheduled(every = "1m")
    public void sendRandomValue() {
        Random random = new Random();
        int value = random.nextInt(100);
        LocalDateTime now = LocalDateTime.now().withSecond(0);
        String formattedDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        EquipmentData data = new EquipmentData(value, formattedDate);
        LOGGER.infof("Enviando dados: %s", data);
        try {
            sendJsonData("http://producer-app:8083/api/api/data", data);
            LOGGER.infof("Dados enviados com sucesso: %s", data);
        } catch (Exception e) {
            LOGGER.errorf("Erro ao enviar dados: %s", e.getMessage());
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