// package org.acme;

// import org.eclipse.microprofile.health.HealthCheck;
// import org.eclipse.microprofile.health.HealthCheckResponse;
// import org.eclipse.microprofile.health.Liveness;
// import jakarta.enterprise.context.ApplicationScoped;
// import org.apache.qpid.jms.JmsConnectionFactory;

// @Liveness
// @ApplicationScoped
// public class ActiveMQHealthCheck implements HealthCheck {

//     private static final String BROKER_URL = "amqp://activemq:5672"; // Endereço do ActiveMQ no Docker

//     @Override
//     public HealthCheckResponse call() {
//         try {
//             JmsConnectionFactory connectionFactory = new JmsConnectionFactory(BROKER_URL);
//             try (jakarta.jms.Connection connection = connectionFactory.createConnection("admin", "admin")) {
//                 connection.start();
//                 return HealthCheckResponse.up("ActiveMQ is up");
//             }
//         } catch (Exception e) {
//             return HealthCheckResponse.down("ActiveMQ is down: " + e.getMessage());
//         }
//     }
// }

