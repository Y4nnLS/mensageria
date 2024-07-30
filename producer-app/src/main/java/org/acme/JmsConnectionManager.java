package org.acme;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class JmsConnectionManager {

    @Inject
    @ConfigProperty(name = "mp.messaging.connector.smallrye-amqp.host")
    private String brokerHost;

    @Inject
    @ConfigProperty(name = "mp.messaging.connector.smallrye-amqp.port")
    private int brokerPort;

    @Inject
    @ConfigProperty(name = "mp.messaging.connector.smallrye-amqp.username")
    private String username;

    @Inject
    @ConfigProperty(name = "mp.messaging.connector.smallrye-amqp.password")
    private String password;

   // @ConfigProperty(name = "quarkus.datasource.jdbc.url")
   private String jdbcUrl = "jdbc:postgresql://postgres:5432/mensageria_DB";

   // @ConfigProperty(name = "quarkus.datasource.username")
   private String dbUsername = "admin";

   // @ConfigProperty(name = "quarkus.datasource.password")
   private String dbPassword = "123";

    private Connection connection;
    private Session session;
    private java.sql.Connection dbConnection; // PostgreSQL Connection

    private static final Logger LOGGER = Logger.getLogger(JmsConnectionManager.class);

    @PostConstruct
    public void init() {
        try {
            // Initialize JMS Connection
            String brokerUrl = String.format("amqp://%s:%d", brokerHost, brokerPort);
            ConnectionFactory connectionFactory = new JmsConnectionFactory(brokerUrl);
            connection = connectionFactory.createConnection(username, password);
            connection.start();

            // Initialize PostgreSQL Connection
            dbConnection = DriverManager.getConnection(jdbcUrl, dbUsername, dbPassword);
            LOGGER.info("PostgreSQL connection established");

        } catch (JMSException | SQLException e) {
            LOGGER.error("Initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws JMSException {
        String brokerUrl = String.format("amqp://%s:%d", brokerHost, brokerPort);
        ConnectionFactory connectionFactory = new JmsConnectionFactory(brokerUrl);
        Connection conn = connectionFactory.createConnection(username, password);
        conn.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        return conn;
    }

    public boolean checkPostgreSQLHealth() {
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                LOGGER.info("PostgreSQL connection is healthy");
                return true;
            }
        } catch (SQLException e) {
            LOGGER.error("PostgreSQL connection check failed: " + e.getMessage());
        }
        return false;
    }

    @PreDestroy
    public void cleanup() {
        if (session != null) {
            try {
                session.close();
            } catch (JMSException e) {
                LOGGER.error("Failed to close JMS session: " + e.getMessage());
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException e) {
                LOGGER.error("Failed to close JMS connection: " + e.getMessage());
            }
        }
        if (dbConnection != null) {
            try {
                dbConnection.close();
            } catch (SQLException e) {
                LOGGER.error("Failed to close PostgreSQL connection: " + e.getMessage());
            }
        }
    }
}
