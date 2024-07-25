FROM amazoncorretto:21

ENV TZ=America/Sao_Paulo
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Criar diretório para a aplicação
RUN mkdir /opt/consumer

# Copiar o JAR gerado pelo Quarkus
COPY consumer-app-1.0.0-SNAPSHOT-runner.jar /opt/consumer/

CMD ["java", "-jar", "/opt/consumer/consumer-app-1.0.0-SNAPSHOT-runner.jar"]
