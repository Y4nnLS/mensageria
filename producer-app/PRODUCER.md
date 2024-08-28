# Producer App

## Visão Geral

O **Producer App** é uma aplicação que publica mensagens em um broker ActiveMQ Artemis e fornece endpoints para receber dados e verificar a saúde do sistema. Ele interage com um banco de dados PostgreSQL e um broker de mensagens ActiveMQ Artemis para completar seu funcionamento.

## Componentes

### 1. ActiveMQ Artemis

- **Descrição**: Um broker de mensagens que facilita a comunicação assíncrona entre componentes através de mensagens.
- **Função**: Recebe e envia mensagens para a aplicação Producer App.
- **Portas**:
  - `1883`: Porta para o protocolo MQTT.
  - `61616`: Porta para o protocolo AMQP.
  - `5672`: Outra porta para o protocolo AMQP.

### 2. PostgreSQL

- **Descrição**: Um banco de dados relacional usado para armazenar dados persistentes.
- **Função**: Armazena dados que podem ser utilizados pela aplicação.
- **Porta**: `5432`

### 3. Producer App

- **Descrição**: A aplicação principal que processa mensagens e publica dados no ActiveMQ.
- **Função**: Recebe dados via HTTP, processa esses dados e publica no canal de mensagens configurado. Também verifica periodicamente a saúde dos serviços que usa (ActiveMQ e PostgreSQL).

## Endpoints da API

### 1. `/api/receive`

- **Método**: `POST`
- **Descrição**: Recebe uma mensagem JSON e confirma o recebimento.
- **Resposta**: "Mensagem recebida com sucesso!" indica que a mensagem foi recebida corretamente.

### 2. `/api/data`

- **Método**: `POST`
- **Descrição**: Recebe dados JSON e publica esses dados no canal de mensagens. Se a aplicação estiver desativada, a mensagem é descartada.
- **Resposta**: 
  - `200 OK` se os dados foram processados e publicados com sucesso.
  - `500 Internal Server Error` se houver um problema ao processar os dados.

### 3. `/api/health/status`

- **Método**: `GET`
- **Descrição**: Retorna o status de saúde da aplicação.
- **Resposta**: JSON com informações sobre a saúde da aplicação.

### 4. `/api/status/{checkName}`

- **Método**: `GET`
- **Descrição**: Retorna o status de um check de saúde específico baseado no nome fornecido.
- **Parâmetro**: `checkName` - Nome do check de saúde.
- **Resposta**: JSON com o status e descrição do check solicitado.

## Monitoramento e Logs

- **Logs**: A aplicação usa o `Logger` para registrar eventos importantes, como o recebimento de mensagens e o status de saúde. Esses logs ajudam a monitorar a operação da aplicação e identificar problemas.
