package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import io.vertx.core.json.JsonObject;

/**
 * A classe MessageConsumer é responsável por consumir e processar mensagens do canal
 * "message-out". Utiliza a API de mensagens reativas do MicroProfile e o Vert.x para
 * manipulação de JSON.
 */
@ApplicationScoped
public class MessageConsumer {

    private static final Logger LOGGER = Logger.getLogger(MessageConsumer.class);

    /**
     * Método que consome mensagens do canal "message-out".
     * 
     * @param jsonObject O objeto JsonObject recebido do canal de mensagens
     * @throws InterruptedException Se ocorrer uma interrupção durante o processamento
     */
    @Incoming("message-out")
    @Transactional
    public void consume(JsonObject jsonObject) throws InterruptedException {
        // Loga a mensagem recebida como JsonObject
        LOGGER.infof("Recebido JsonObject: %s", jsonObject.encode());

        try {
            // Converte o JsonObject para um objeto EquipmentData
            EquipmentData data = jsonObject.mapTo(EquipmentData.class);
            LOGGER.infof("Dados convertidos para o objeto: %s", data);

            // Salva os dados no banco de dados
            data.persist();
            LOGGER.info("Dados salvos com sucesso no banco de dados");

        } catch (Exception e) {
            // Loga qualquer erro que ocorra durante o processamento e armazenamento dos dados
            LOGGER.errorf("Erro ao processar e armazenar dados: %s", e.getMessage());
        }
    }
}
