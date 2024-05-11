package com.example.smartgaugecontrol;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ekn.gruzer.gaugelibrary.HalfGauge;
import com.ekn.gruzer.gaugelibrary.Range;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private HalfGauge halfGauge;
    private TextView textViewStatus;

    private MqttClient arduinoClient;
    private static final String ARDUINO_MQTT_TOPIC = "BehYNK2qm%QRo5Wwm@8ouJ"; // Tópico MQTT para o dispositivo Arduino

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        halfGauge = findViewById(R.id.halfGauge);
        textViewStatus = findViewById(R.id.textViewStatus);

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
        halfGauge.setValue(20.0); // Valor inicial do medidor

        textViewStatus.setText("Conectando ao servidor MQTT...");

        setupArduinoMqttClient();
    }

    private void setupArduinoMqttClient() {
        try {
            String clientId = MqttClient.generateClientId();
            arduinoClient = new MqttClient("tcp://public.mqtthq.com:1883", clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            arduinoClient.connect(options);
            arduinoClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    updateStatusText("Conexão MQTT (Arduino) perdida.");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String receivedMessage = new String(message.getPayload(), StandardCharsets.UTF_8);
                    //updateStatusText("Mensagem recebida do dispositivo Arduino: " + receivedMessage);

                    // Ajustar a temperatura com base no número recebido
                    try {
                        // Extrair o valor numérico da temperatura da mensagem
                        String[] parts = receivedMessage.split(":");
                        if (parts.length == 2) {
                            double receivedTemperature = Double.parseDouble(parts[1].trim());
                            halfGauge.setValue(receivedTemperature);
                            updateStatusText("Temperatura ajustada: " + receivedTemperature, 5000);
                        } else {
                            updateStatusText("Mensagem recebida com formato inválido: " + receivedMessage);
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        updateStatusText("Falha ao converter temperatura: " + receivedMessage);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Não é necessário neste caso
                }
            });

            // Inscreva-se no tópico correto para receber mensagens do Arduino
            arduinoClient.subscribe(ARDUINO_MQTT_TOPIC);

            // Limpe o texto após 5 segundos
            new Handler().postDelayed(() -> textViewStatus.setText(""), 5000);

            updateStatusText("Conectado ao servidor MQTT (Arduino).");
        } catch (Exception e) {
            updateStatusText("Falha ao conectar ao servidor MQTT (Arduino).");
            e.printStackTrace();
        }
    }

    private void updateStatusText(String message) {
        runOnUiThread(() -> textViewStatus.append("\n" + message));
    }

    private void showToast(String message, int duration) {
        runOnUiThread(() -> {
            Toast toast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT);
            toast.show();

            // Define um atraso antes de cancelar o Toast
            new Handler().postDelayed(toast::cancel, duration);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (arduinoClient != null && arduinoClient.isConnected()) {
                arduinoClient.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
