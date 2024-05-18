
# 📊 SmartGaugeControl

Este projeto é uma solução completa para o monitoramento de temperatura, utilizando um sofisticado gráfico de medidor (Gauge Chart) como interface de visualização. O dispositivo principal é um ESP32, que se conecta a um servidor MQTT para receber informações em tempo real sobre a temperatura simulada.

O medidor é acompanhado por uma faixa de LEDs que oferece uma representação visual imediata da temperatura medida. Com um design elegante e intuitivo, os LEDs mudam de cor conforme a temperatura, proporcionando uma experiência visual envolvente. Além disso, há LEDs que indicam a conexão com a internet e o servidor MQTT.

## 🛠️ Ferrramentas usadas 

Para desenvolver este projeto, utilizamos diversas ferramentas que nos auxiliaram em diferentes etapas do processo. Aqui estão as principais:

- 🌐 MQTTHQ: Um servidor MQTT público utilizado para comunicação entre o ESP32 e outros serviços.

- 💻 Wokwi: Um emulador de hardware usado para simular o ambiente Arduino e testar o código antes da implementação no hardware real.

- 📱 Android Studio: Utilizado para desenvolver a aplicação Android responsável por se comunicar com o ESP32, exibir os dados e controlar o aumento ou diminuição da temperatura

Essas ferramentas foram essenciais para garantir o desenvolvimento eficiente e funcional do projeto, proporcionando ambientes de teste e comunicação necessários para o sucesso da implementação


## 📋 Funcionalidades
 
Este projeto oferece uma série de funcionalidades que incluem:

### 🌡️ Coleta de Dados Ambientais
| Funcionalidade  |     | Descrição                           |
| :---------- | :--------- | :---------------------------------- |
| `Medir Temperatura e Umidade` |  | O projeto utiliza um sensor DHT22 para medir a temperatura e a umidade do ambiente.|
| `Exibição Local` |  | Um display LCD I2C é usado para exibir os valores medidos de temperatura e umidade em tempo real. |   



### 📡 Transmissão de Dados via MQTT
| Funcionalidade  |     | Descrição                           |
| :---------- | :--------- | :---------------------------------- |
| `Publicação de Dados para um Servidor MQTT` |  |O projeto transmite os valores de temperatura e umidade para um servidor MQTT, permitindo monitoramento remoto e integração com outras aplicações IoT.|
| `Formato de Dados JSON` |  | Os dados são formatados como JSON antes de serem enviados para o servidor MQTT, facilitando a interoperabilidade e a análise de dados. |  

### 🔁 Interação com o Servidor MQTT
| Funcionalidade  |     | Descrição                           |
| :---------- | :--------- | :---------------------------------- |
| `Recebimento de Mensagens MQTT` |  |O projeto pode receber mensagens do servidor MQTT e exibi-las no LCD. Isso permite interações bidirecionais, como comandos remotos ou notificações.|
| `Reconexão Automática ao MQTT` |  | Se a conexão com o servidor MQTT for interrompida, o projeto tenta reconectar automaticamente, garantindo a continuidade do serviço. |  ´

### 🚥 Controle Remoto de Dispositivos
| Funcionalidade  |     | Descrição                           |
| :---------- | :--------- | :---------------------------------- |
| `Controle de Dispositivos Remotos` |  |Além de receber mensagens do servidor MQTT, o projeto pode enviar comandos para controlar dispositivos remotos, como ativar ou desativar luzes ou dispositivos de HVAC.|

### 📊 Análise e Armazenamento de Dados
| Funcionalidade  |     | Descrição                           |
| :---------- | :--------- | :---------------------------------- |
| `Análise Local de Dados` |  |Além de transmitir dados para o servidor MQTT, o projeto pode analisar e armazenar dados localmente para futura referência ou análise.|
| `Visualização Gráfica` |  | Os dados coletados podem ser visualizados em gráficos ou dashboards para uma análise mais detalhada e uma compreensão mais profunda das tendências de temperatura e umidade ao longo do tempo.|  


### 🛠️ Outras Funcionalidades
| Funcionalidade  |     | Descrição                           |
| :---------- | :--------- | :---------------------------------- |
| `Conexão Wi-Fi` |  |O projeto se conecta a uma rede Wi-Fi para comunicação com o servidor MQTT.|
| `Indicadores Visuais` |  |  O LCD mostra mensagens para indicar o status da conexão Wi-Fi e do servidor MQTT, bem como para exibir mensagens recebidas.|  

## 🔧 Configurações Necessárias

Para rodar esse projeto, você vai precisar adicionar ou mudar as seguintes coisas 

🔗 `MQTT_BROKER`: Endereço do servidor MQTT.


🗣️ `MQTT_TOPIC`: Tópico MQTT para comunicação.


## 🔧 Configurações do projeto

Além das configurações mencionadas acima, você também precisa adicionar as seguintes dependências ao seu projeto:

```java 
dependencies {
    implementation ("com.github.Gruzer:simple-gauge-android:0.3.1")
    implementation ("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation("com.hivemq:hivemq-mqtt-client:1.2.1") ...
 Outras dependências...
```

Além disso, nas configurações do projeto, é necessário adicionar o seguinte trecho de código para garantir que as dependências sejam resolvidas corretamente:

 ```java 
 dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
 ```

Apos essa breve configuração podemos definir de fato o Gauge Chart seguindo o modelo similhar a esse 
```java 
 Range range1 = new Range();
        range1.setColor(Color.parseColor("#0000FF")); // Cor para ambiente frio
        range1.setFrom(-40.0); // Começa em -40
        range1.setTo(0.0); // Termina em 0

        Range range2 = new Range();
        range2.setColor(Color.parseColor("#008000")); // Cor para temperatura agradável
        range2.setFrom(0.0); // Começa em 0
        range2.setTo(40.0); // Termina em 40

        Range range3 = new Range();
        range3.setColor(Color.parseColor("#FF0000")); // Cor para calor
        range3.setFrom(40.0); // Começa em 40
        range3.setTo(80.0); // Termina em 80

        halfGauge.addRange(range1);
        halfGauge.addRange(range2);
        halfGauge.addRange(range3);

        halfGauge.setMinValue(-40.0); // Valor mínimo do medidor
        halfGauge.setMaxValue(80.0); // Valor máximo do medidor
        double initialValue = setInitialRandomGaugeValue(); // Define um valor inicial aleatório dentro do intervalo permitido
        publishInitialValueToArduino(initialValue); // Publica o valor inicial aleatório no servidor MQTT

```
## Projeto

![Arduino](/img/arduino.png)

![server](/img/server.png)

## Rodando os testes

esse é um exempro do projeto funcionando  



https://github.com/WesleyS08/SmartGaugeControl/assets/113936684/53243e08-62ae-4adc-8154-5d221799128b


## Rodando os testes em um Habiente Real


![server](/img/IMG_20240518_200257_076_LL.jpg)
## Referência

- Documentação do Simple Gauge Android: Documentação oficial do [Simple Gauge Android](https://github.com/Gruzer/simple-gauge-android), uma biblioteca utilizada neste projeto para criar o gráfico de medidor.

- Projeto da Larissa - [Repositório GitHub](https://github.com/LarissaSL/Gauge_Chart_Game) com o projeto da Larissa, utilizando o Gauge Chart como interface de visualização.

## Licença

[MIT](https://choosealicense.com/licenses/mit/)

