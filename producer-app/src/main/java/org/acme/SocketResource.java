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
        LOGGER.info("Mensagem recebida: " + jsonMessage);
        return "Mensagem recebida com sucesso!";
    }

    @POST
    @Path("/data")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response receiveData(String jsonString) {
        if (!ServiceState.isActive()) {
            System.out.println(jsonString);
            LOGGER.warn("A rota está desativada. Mensagem recebida será descartada.");
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("A rota está desativada no momento.")
                           .build();
        }

        LOGGER.infof("Recebendo JSON: %s", jsonString);

        try {
            EquipmentData data = objectMapper.readValue(jsonString, EquipmentData.class);
            LOGGER.infof("Dados convertidos para o objeto: %s", data);

            emitter.send(data).whenComplete((result, ex) -> {
                if (ex != null) {
                    LOGGER.errorf("Erro ao enviar dados para o canal: %s", ex.getMessage());
                } else {
                    LOGGER.infof("Dados enviados com sucesso: %s", data);
                }
            });

            return Response.ok().build();
        } catch (Exception e) {
            LOGGER.errorf("Erro ao processar os dados: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Erro ao processar os dados")
                           .build();
        }
    }
}
