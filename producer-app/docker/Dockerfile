FROM amazoncorretto:21

ENV TZ=America/Sao_Paulo
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Criar diretório para a aplicação
RUN mkdir /opt/producer

# Copiar o JAR gerado pelo Quarkus
COPY producer-app-1.0.0-SNAPSHOT-runner.jar /opt/producer/

CMD ["java", "-jar", "/opt/producer/producer-app-1.0.0-SNAPSHOT-runner.jar"]
