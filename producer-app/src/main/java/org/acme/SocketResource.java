package org.acme;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/api")
public class SocketResource {

    private static final Logger LOGGER = Logger.getLogger(SocketResource.class);

    @Channel("message-out")
    Emitter<EquipmentData> emitter;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @POST
    @Path("/receive")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String receiveMessage(String jsonMessage) {
        // Retorna uma resposta simples indicando sucesso
        System.out.println("\n\n" + jsonMessage + "\n\n");
        return "Mensagem recebida com sucesso!";
    }

    @POST
    @Path("/data")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response receiveData(String jsonString) {
        LOGGER.infof("Recebendo JSON: %s", jsonString);

        try {
            // Converte o JSON para o objeto EquipmentData
            EquipmentData data = objectMapper.readValue(jsonString, EquipmentData.class);
            LOGGER.infof("Dados convertidos para o objeto: %s", data);

            // Log para verificação do estado do emitter antes do envio
            LOGGER.info("Preparando para enviar os dados para o canal 'message-out'.");

            // Envia os dados para o canal
            emitter.send(data);

            // Log após o envio bem-sucedido
            LOGGER.infof("Dados enviados com sucesso: %s", data);

            // Retorna resposta de sucesso
            return Response.ok().build();
        } catch (Exception e) {
            // Log em caso de erro ao converter ou enviar dados
            LOGGER.errorf("Erro ao processar os dados: %s", e.getMessage());

            // Retorna resposta de erro
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Erro ao processar os dados")
                           .build();
        }
    }
}
