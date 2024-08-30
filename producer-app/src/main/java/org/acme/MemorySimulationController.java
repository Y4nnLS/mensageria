package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

/**
 * MemorySimulationController fornece endpoints REST
 * que permitem iniciar e parar a simulação de erro de memória heap.
 * 
 * Esta classe expõe dois endpoints: 
 * 1. /api/memory/start: para iniciar a simulação de erro de memória.
 * 2. /api/memory/stop: para parar a simulação de erro de memória.
 */
@Path("/memory")
public class MemorySimulationController {

    @Inject
    @Any
    @Readiness
    HeapMemoryHealthCheck heapMemoryHealthCheck;

    /**
     * Endpoint para iniciar a simulação de erro de memória.
     * 
     * Este método ativa a simulação de uso excessivo de memória heap
     * que pode eventualmente resultar em um OutOfMemoryError.
     * 
     * @return Response com mensagem indicando que a simulação de memória foi iniciada.
     */
    @GET
    @Path("/start")
    @Produces(MediaType.TEXT_PLAIN)
    public Response startSimulation() {
        // Ativa a simulação de erro de memória.
        heapMemoryHealthCheck.setSimulationActive(true);
        System.out.println("\n\n\nIniciando simulação de memória.\n\n\n");
        return Response.ok("Simulação de memória iniciada.").build();
    }

    /**
     * Endpoint para parar a simulação de erro de memória.
     * 
     * Este método desativa a simulação de uso excessivo de memória heap,
     * retornando o sistema ao seu estado normal de operação.
     * 
     * @return Response com mensagem indicando que a simulação de memória foi encerrada.
     */
    @GET
    @Path("/stop")
    @Produces(MediaType.TEXT_PLAIN)
    public Response stopSimulation() {
        // Desativa a simulação de erro de memória.
        heapMemoryHealthCheck.setSimulationActive(false);
        System.out.println("\n\n\nEncerrando simulação de memória.\n\n\n");
        return Response.ok("Simulação de memória encerrada.").build();
    }
}
