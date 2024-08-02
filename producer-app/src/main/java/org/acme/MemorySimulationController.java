package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

@Path("/memory")
public class MemorySimulationController {

    @Inject
    @Any
    @Readiness
    HeapMemoryHealthCheck heapMemoryHealthCheck;

    @GET
    @Path("/start")
    @Produces(MediaType.TEXT_PLAIN)
    public Response startSimulation() {
        heapMemoryHealthCheck.setSimulationActive(true);
        System.out.println("\n\n\nIniciando simulação de memória.\n\n\n");
        return Response.ok("Memory simulation started.").build();
    }

    @GET
    @Path("/stop")
    @Produces(MediaType.TEXT_PLAIN)
    public Response stopSimulation() {
        heapMemoryHealthCheck.setSimulationActive(false);
        System.out.println("\n\n\nEncerrando simulação de memória.\n\n\n");
        return Response.ok("Memory simulation stopped.").build();
    }
}
