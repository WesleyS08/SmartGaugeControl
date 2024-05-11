package com.example.smartgaugecontrol;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.ekn.gruzer.gaugelibrary.HalfGauge;
import com.ekn.gruzer.gaugelibrary.Range;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private HalfGauge halfGauge;
    private Button btnAumentar;
    private Button btnDiminuir;
    private TextView textViewStatus;

    private Mqtt5BlockingClient client;
    private static final String TOPIC = "BehYNK2qm%QRo5Wwm@8ouJ"; // Tópico correto

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar as views
        halfGauge = findViewById(R.id.halfGauge);
        btnAumentar = findViewById(R.id.btnAumentar);
        btnDiminuir = findViewById(R.id.btnDiminuir);
        textViewStatus = findViewById(R.id.textViewStatus);

        // Configurar faixas de cores e valores mínimo e máximo
        Range range1 = new Range();
        range1.setColor(Color.parseColor("#0000FF")); //  temperaturas baixas
        range1.setFrom(0.0);
        range1.setTo(15.0);

        Range range2 = new Range();
        range2.setColor(Color.parseColor("#008000")); // temperaturas médias
        range2.setFrom(15.0);
        range2.setTo(25.0);

        Range range3 = new Range();
        range3.setColor(Color.parseColor("#FF0000")); // temperaturas altas
        range3.setFrom(25.0);
        range3.setTo(40.0);

        // Adicionar faixas de cores ao medidor
        halfGauge.addRange(range1);
        halfGauge.addRange(range2);
        halfGauge.addRange(range3);

        // Definir valores mínimo, máximo e atual
        halfGauge.setMinValue(0.0);
        halfGauge.setMaxValue(40.0);
        halfGauge.setValue(20.0); // Definir temperatura inicial

        // Exibir mensagem indicando que está conectando ao servidor MQTT
        textViewStatus.setText("Conectando ao servidor MQTT...");

        // Configurar o cliente MQTT
        setupMqttClient();

        // Configurar o listener de clique para os botões
        btnAumentar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aumentarTemperatura();
            }
        });

        btnDiminuir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                diminuirTemperatura();
            }
        });

        // Inscrever-se no tópico MQTT para receber informações de temperatura
        subscribeToMqttTopic();
    }

    private void setupMqttClient() {
        String host = "c023193fa6834fdaa25f0b48c176dd69.s1.eu.hivemq.cloud";
        String username = "Wesley1.0";
        String password = "Ws58247889!";

        client = Mqtt5Client.builder()
                .serverHost(host) // Define o host do servidor
                .serverPort(8883) // Define a porta do servidor
                .sslWithDefaultConfig() // Configuração padrão para SSL
                .buildBlocking(); // Constrói o cliente de forma bloqueante

        client.connectWith()
                .simpleAuth() // Usa autenticação simples
                .username(username) // Define o nome de usuário
                .password(StandardCharsets.UTF_8.encode(password)) // Define a senha
                .applySimpleAuth() // Aplica autenticação
                .send(); // Envia a conexão

        // Exibir mensagem de conexão bem-sucedida
        textViewStatus.setText("Conectado ao servidor MQTT.");

        // Desaparecer a mensagem após 2 segundos
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                textViewStatus.setText(""); // Limpar o texto após 2 segundos
            }
        }, 2000);
    }

    // Método para aumentar a temperatura
    private void aumentarTemperatura() {
        double currentValue = halfGauge.getValue();
        double newValue = currentValue + 5.0; // Aumentar a temperatura em 5 unidades
        if (newValue > halfGauge.getMaxValue()) {
            newValue = halfGauge.getMaxValue(); // Garantir que não ultrapasse o valor máximo
        }
        halfGauge.setValue(newValue);

        // Enviar temperatura atualizada para o servidor MQTT
        publishTemperature(newValue);
    }

    // Método para diminuir a temperatura
    private void diminuirTemperatura() {
        double currentValue = halfGauge.getValue();
        double newValue = currentValue - 5.0; // Diminuir a temperatura em 5 unidades
        if (newValue < halfGauge.getMinValue()) {
            newValue = halfGauge.getMinValue(); // Garantir que não fique abaixo do valor mínimo
        }
        halfGauge.setValue(newValue);

        // Enviar temperatura atualizada para o servidor MQTT
        publishTemperature(newValue);
    }

    // Método para enviar a temperatura para o servidor MQTT
    private void publishTemperature(double temperature) {
        if (client != null && client.getState().isConnected()) {
            // Adiciona um prefixo à mensagem para identificá-la como enviada pelo aplicativo
            String message = "APP_MSG:" + Double.toString(temperature);

            client.publishWith()
                    .topic(TOPIC) // Define o tópico MQTT
                    .payload(message.getBytes()) // Define a carga útil da mensagem
                    .send(); // Envia a mensagem

            // Exibir mensagem de confirmação
            updateStatusText("Temperatura enviada: " + temperature);
        } else {
            // Reconectar ao servidor MQTT se o cliente não estiver conectado
            setupMqttClient();
        }
    }

    // Método para se inscrever em um tópico MQTT e configurar um listener para mensagens recebidas
    private void subscribeToMqttTopic() {
        // O cliente HiveMQ Cloud já está configurado para se inscrever no tópico no método setupMqttClient()
        // Nenhuma ação adicional necessária aqui
    }

    // Método para receber mensagens MQTT
    private void setupMqttMessageReceiver() {
        client.toAsync().subscribeWith()
                .topicFilter(TOPIC)
                .callback(publish -> {
                    // Mensagem recebida
                    String receivedMessage = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);

                    // Verifica se a mensagem contém o prefixo do identificador do aplicativo
                    if (!receivedMessage.startsWith("APP_MSG:")) {
                        // A mensagem não foi enviada pelo aplicativo, processa-a normalmente
                        // Exibir a mensagem recebida
                        showToast("Mensagem recebida: " + receivedMessage);

                        // Parse da temperatura recebida
                        double newTemperature = Double.parseDouble(receivedMessage);

                        // Atualizar o valor do HalfGauge com a nova temperatura recebida
                        runOnUiThread(() -> halfGauge.setValue(newTemperature));

                        // Verificar a diferença entre a temperatura atual e a nova temperatura recebida
                        double currentTemperature = halfGauge.getValue();
                        double temperatureDifference = Math.abs(newTemperature - currentTemperature);

                        // Modificar o tempo de atraso do Handler com base na diferença de temperatura
                        int delayMillis = calculateDelayMillis(temperatureDifference);

                        // Reiniciar o Handler com o novo tempo de atraso
                        Handler handler = new Handler();
                        handler.removeCallbacksAndMessages(null); // Remove todas as mensagens pendentes
                        handler.postDelayed(() -> {
                            // Ação a ser realizada após o tempo de atraso
                            // Aqui você pode fazer qualquer coisa que desejar, com base na diferença de temperatura
                            // Por exemplo, atualizar a interface do usuário, executar uma animação, etc.
                        }, delayMillis);
                    }
                }).send();
    }

    // Método para calcular o tempo de atraso do Handler com base na temperatura recebida
    private int calculateDelayMillis(double temperatureDifference) {
        // Implemente sua lógica para calcular o tempo de atraso com base na diferença de temperatura aqui
        // Por exemplo, você pode aumentar o atraso se a diferença de temperatura for grande, ou diminuí-lo se for pequena
        // Este é apenas um exemplo de implementação simples, você pode ajustá-lo conforme necessário
        return (int) (temperatureDifference * 1000); // Converte a diferença de temperatura em milissegundos
    }

    // Método auxiliar para atualizar o texto da TextView
    private void updateStatusText(String message) {
        runOnUiThread(() -> textViewStatus.setText(message));
    }

    // Método auxiliar para exibir Toast
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Configurar o recebimento de mensagens MQTT ao retomar a atividade
        setupMqttMessageReceiver();
    }

    // Método para desconectar do servidor MQTT
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (client != null && client.getState().isConnected()) {
            client.disconnect();
        }
    }
}
