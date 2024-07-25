package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class MessageConsumer {

    private static final Logger LOGGER = Logger.getLogger(MessageConsumer.class);

    @Incoming("message-out")
    @Transactional
    public void consume(JsonObject jsonObject) throws InterruptedException {
        // Log a mensagem recebida como JsonObject
        LOGGER.infof("Recebido JsonObject: %s", jsonObject.encode());

        try {
            // Converte JsonObject para EquipmentData
            EquipmentData data = jsonObject.mapTo(EquipmentData.class);
            LOGGER.infof("Dados convertidos para o objeto: %s", data);

            // Salva os dados no banco de dados
            data.persist();
            LOGGER.info("Dados salvos com sucesso no banco de dados");

        } catch (Exception e) {
            LOGGER.errorf("Erro ao processar e armazenar dados: %s", e.getMessage());
        }
    }
}
