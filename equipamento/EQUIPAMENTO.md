# Equipment App

## Visão Geral

O **Equipment App** é uma aplicação que coleta dados aleatórios e os envia periodicamente para o Producer App. A aplicação também tenta reenviar mensagens falhadas que não foram enviadas com sucesso na tentativa inicial.

## Funcionalidade

- **Envio de Dados Aleatórios**: A cada minuto, a aplicação gera um valor aleatório e o envia para o endpoint do Producer App.
- **Reenvio de Mensagens Falhadas**: A cada minuto, a aplicação tenta reenviar mensagens que falharam ao serem enviadas anteriormente.

## Componentes

### 1. `sendRandomValue()`

- **Descrição**: Método agendado que é executado a cada 1 minuto para gerar um valor aleatório, criar um objeto `EquipmentData` e enviá-lo para o Producer App.
- **Funcionalidade**:
  - Gera um valor aleatório.
  - Cria um objeto `EquipmentData` com o valor e a data/hora atual.
  - Tenta enviar os dados para o Producer App.
  - Adiciona os dados à lista de falhas se a tentativa de envio falhar.

### 2. `retryFailedMessages()`

- **Descrição**: Método agendado que tenta reenviar mensagens que falharam ao serem enviadas anteriormente.
- **Funcionalidade**:
  - Itera sobre a lista de mensagens falhadas.
  - Tenta reenviar cada mensagem.
  - Se o envio falhar, a mensagem é adicionada de volta à lista de falhas.

### 3. `sendJsonData(String targetUrl, EquipmentData data)`

- **Descrição**: Envia dados em formato JSON para o endpoint especificado.
- **Parâmetros**:
  - `targetUrl`: URL do endpoint para onde os dados serão enviados.
  - `data`: Objeto `EquipmentData` a ser enviado.
- **Exceção**: Lança uma exceção se o código de resposta não for HTTP 200 OK.

### 4. `convertToJson(EquipmentData data)`

- **Descrição**: Converte o objeto `EquipmentData` em uma string JSON.
- **Parâmetro**: 
  - `data`: Objeto `EquipmentData` a ser convertido.
- **Retorno**: String JSON representando o objeto `EquipmentData`.

## Monitoramento e Logs

- **Logs**: A aplicação usa o `Logger` para registrar eventos importantes, como o envio de dados e a tentativa de reenvio de mensagens falhadas. Os logs ajudam a monitorar a operação da aplicação e a diagnosticar problemas.
