FROM amazoncorretto:21

ENV TZ=America/Sao_Paulo
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Criar diretório para a aplicação
RUN mkdir /opt/equipamento

# Copiar o JAR gerado pelo Quarkus
COPY equipamento-1.0.0-SNAPSHOT-runner.jar /opt/equipamento/

CMD ["java", "-jar", "/opt/equipamento/equipamento-1.0.0-SNAPSHOT-runner.jar"]
