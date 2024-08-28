# Equipment App

## Visão Geral

O **Equipment App** é uma aplicação que coleta dados aleatórios e os envia periodicamente para o Producer App. Além disso, processa mensagens recebidas de um canal de mensagens e as armazena em um banco de dados.

## Funcionalidade

### 1. `EquipmentScheduler`

- **Descrição**: Agendador que gera valores aleatórios e os envia para o Producer App a cada minuto. Tenta reenviar mensagens falhadas a cada minuto.
- **Métodos**:
  - `sendRandomValue()`: Gera e envia dados aleatórios.
  - `retryFailedMessages()`: Reenvia mensagens falhadas.
  - `sendJsonData(String targetUrl, EquipmentData data)`: Envia dados em formato JSON para um endpoint.
  - `convertToJson(EquipmentData data)`: Converte um objeto `EquipmentData` para uma string JSON.

### 2. `MessageConsumer`

- **Descrição**: Consumidor de mensagens que processa e armazena dados recebidos do canal `message-out`.
- **Métodos**:
  - `consume(JsonObject jsonObject)`: 
    - **Descrição**: Recebe mensagens do canal `message-out`, converte de `JsonObject` para `EquipmentData` e armazena no banco de dados.
    - **Parâmetros**:
      - `jsonObject`: Mensagem recebida no formato `JsonObject`.
    - **Exceção**: Lança uma exceção se houver um erro ao processar ou armazenar os dados.

## Componentes

### `EquipmentScheduler`

- **URL do Producer App**: `http://producer-app:8083/api/api/data` — Endpoint para onde os dados são enviados.
- **Armazenamento de Mensagens Falhadas**: Mensagens que falham ao ser enviadas são armazenadas e tentadas novamente a cada minuto.

### `MessageConsumer`

- **Canal de Mensagens**: `message-out` — Canal do qual as mensagens são consumidas.
- **Processamento**:
  - Converte `JsonObject` para `EquipmentData`.
  - Salva `EquipmentData` no banco de dados.

## Monitoramento e Logs

- **Logs**: A aplicação utiliza `Logger` para registrar informações sobre o envio e recebimento de dados, bem como erros encontrados durante o processamento.

## Exemplo de Uso

- **Envio de Dados**: O `EquipmentScheduler` envia dados aleatórios periodicamente.
- **Processamento de Mensagens**: O `MessageConsumer` processa e armazena mensagens recebidas no canal `message-out`.