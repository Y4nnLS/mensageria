package org.acme;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/api")
public class SocketResource {

    private static final Logger LOGGER = Logger.getLogger(SocketResource.class);

    @Channel("message-out") // Canal para enviar mensagens para o sistema de mensageria
    Emitter<EquipmentData> emitter;

    private final ObjectMapper objectMapper = new ObjectMapper(); // Para conversão entre JSON e objetos
    private static final String HEALTH_URL = "http://localhost:8083/api/q/health/ready"; // URL para verificar o status de saúde

    /**
     * È apenas uma rota de teste que criei para verificar se estava recebendo as mensagens corretamente.
     * 
     * Recebe uma mensagem JSON e retorna uma confirmação de recebimento.
     * 
     * Este método é responsável por receber mensagens no formato JSON e
     * registrar essas mensagens no log. A resposta indica que a mensagem
     * foi recebida com sucesso.
     * 
     * @param jsonMessage A mensagem recebida em formato JSON.
     * @return Uma confirmação de que a mensagem foi recebida com sucesso.
     */
    @POST
    @Path("/receive") // Define o caminho para este método POST
    @Consumes(MediaType.APPLICATION_JSON) // Define que o método consome JSON
    @Produces(MediaType.TEXT_PLAIN) // Define que o método produz texto simples
    public String receiveMessage(String jsonMessage) {
        LOGGER.info("Mensagem recebida: " + jsonMessage);
        return "Mensagem recebida com sucesso!"; // Retorna uma mensagem de sucesso
    }

    /**
     * Recebe dados em formato JSON, os converte para um objeto e os envia
     * para um canal de mensagens.
     * 
     * Este método processa o JSON recebido, converte-o para um objeto
     * `EquipmentData`, e envia esse objeto para um canal de mensagens.
     * Se a aplicação estiver desativada, a mensagem será descartada.
     * 
     * @param jsonString A string JSON representando os dados recebidos.
     * @return Uma resposta indicando o sucesso ou falha no processamento.
     */
    @POST
    @Path("/data") // Define o caminho para este método POST
    @Consumes(MediaType.APPLICATION_JSON) // Define que o método consome JSON
    public Response receiveData(String jsonString) {
        if (!ServiceState.isApplicationActive()) {
            System.out.println(jsonString);
            LOGGER.warn("A rota está desativada. Mensagem recebida será descartada.");
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("A rota está desativada no momento.")
                    .build(); // Retorna uma resposta indicando que a rota está desativada
        }

        LOGGER.infof("Recebendo JSON: %s", jsonString);

        try {
            EquipmentData data = objectMapper.readValue(jsonString, EquipmentData.class); // Converte JSON para objeto
            LOGGER.infof("Dados convertidos para o objeto: %s", data);

            emitter.send(data).whenComplete((result, ex) -> { // Envia dados para o canal
                if (ex != null) {
                    LOGGER.errorf("Erro ao enviar dados para o canal: %s", ex.getMessage());
                } else {
                    LOGGER.infof("Dados enviados com sucesso: %s", data);
                }
            });

            return Response.ok().build(); // Retorna uma resposta de sucesso
        } catch (Exception e) {
            LOGGER.errorf("Erro ao processar os dados: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao processar os dados")
                    .build(); // Retorna uma resposta indicando erro no processamento
        }
    }

    /**
     * Obtém o status de saúde da aplicação.
     * 
     * Este método faz uma requisição para um endpoint de saúde e retorna
     * o status de saúde como JSON.
     * 
     * @return Uma resposta contendo o status de saúde da aplicação em formato JSON.
     */
    @GET
    @Path("/health/status") // Define o caminho para este método GET
    @Produces(MediaType.APPLICATION_JSON) // Define que o método produz JSON
    public Response getHealthStatus() {
        String healthStatus = fetchHealthStatus(); // Obtém o status de saúde
        return Response.ok(healthStatus, MediaType.APPLICATION_JSON).build(); // Retorna o status de saúde
    }

    /**
     * Obtém o status de uma verificação específica de saúde.
     * 
     * Este método faz uma requisição para um endpoint de saúde, procura
     * por uma verificação específica pelo nome, e retorna o status dessa
     * verificação.
     * 
     * @param checkName O nome da verificação de saúde para a qual o status deve ser retornado.
     * @return Uma resposta contendo o status da verificação solicitada.
     */
    @GET
    @Path("/status/{checkName}") // Define o caminho para este método GET, com um parâmetro de caminho
    @Produces(MediaType.APPLICATION_JSON) // Define que o método produz JSON
    public Response getHealthCheckStatus(@PathParam("checkName") String checkName) {
        try {
            String healthStatus = fetchHealthStatus(); // Obtém o status de saúde
            JsonNode healthStatusNode = objectMapper.readTree(healthStatus); // Converte a resposta para um objeto JSON
            JsonNode checks = healthStatusNode.get("checks"); // Obtém a lista de verificações

            for (JsonNode check : checks) {
                if (check.get("name").asText().equals(checkName)) { // Encontra a verificação com o nome especificado
                    return Response.ok(check).build(); // Retorna o status da verificação solicitada
                }
            }
            return Response.ok("Não encontrado").build(); // Retorna uma resposta se a verificação não for encontrada
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Erro ao processar o JSON").build(); // Retorna erro se ocorrer uma exceção
        }
    }

    /**
     * Faz uma requisição HTTP para obter o status de saúde da aplicação.
     * 
     * Este método cria um cliente HTTP, envia uma requisição GET para um
     * endpoint de saúde, e retorna o corpo da resposta.
     * 
     * @return O corpo da resposta da requisição GET ao endpoint de saúde, ou null se houver um erro.
     */
    private String fetchHealthStatus() {
        try {
            // Criando um cliente HTTP
            HttpClient client = HttpClient.newHttpClient();

            // Criando a requisição
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HEALTH_URL))
                    .GET() // Método GET
                    .build();

            // Enviando a requisição e recebendo a resposta
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.body(); // Retorna o corpo da resposta
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Retorna null em caso de erro
    }
}
