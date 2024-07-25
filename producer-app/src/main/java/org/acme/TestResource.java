package org.acme;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/test")
public class TestResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String test() {
        return "Service is up!";
    }

    @POST
    @Path("/receive")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String receiveMessage(String jsonMessage) {
        // Retorna uma resposta simples indicando sucesso
        System.out.println("\n\n" + jsonMessage + "\n\n");
        return "Mensagem recebida com sucesso!";
    }
}
