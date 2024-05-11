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
    private Button btnAumentar;
    private Button btnDiminuir;
    private TextView textViewStatus;

    private MqttClient androidClient;
    private static final String ANDROID_MQTT_TOPIC = "BehYNK2qm%QRo5Wwm@8ouJ"; // Tópico MQTT para o servidor Android

    private MqttClient arduinoClient;
    private static final String ARDUINO_MQTT_TOPIC = "BehYNK2qm%QRo5Wwm@8ouJ"; // Tópico MQTT para o dispositivo Arduino

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        halfGauge = findViewById(R.id.halfGauge);
        btnAumentar = findViewById(R.id.btnAumentar);
        btnDiminuir = findViewById(R.id.btnDiminuir);
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

        textViewStatus.setText("Conectando aos servidores MQTT...");

        setupArduinoMqttClient();

        btnAumentar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aumentarTemperatura();
                publishToAndroid();
                publishToArduino();
            }
        });

        btnDiminuir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                diminuirTemperatura();
                publishToAndroid();
                publishToArduino();
            }
        });

        subscribeToAndroidMqttTopic();
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
                    updateStatusText("Mensagem recebida do dispositivo Arduino: " + receivedMessage);

                    // Verifique se a mensagem está no formato esperado
                    if (receivedMessage.startsWith("APP_MSG:")) {
                        // Extraia o valor da temperatura da mensagem
                        String temperatureStr = receivedMessage.substring(8);
                        try {
                            double receivedTemperature = Double.parseDouble(temperatureStr.trim());
                            // Atualize o valor do halfGauge
                            runOnUiThread(() -> halfGauge.setValue(receivedTemperature));
                            // Exiba a mensagem de atualização de temperatura
                            showToast("Temperatura atualizada: " + receivedTemperature, 5000);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            updateStatusText("Falha ao converter temperatura: " + temperatureStr);
                        }
                    } else {
                        updateStatusText("Mensagem recebida inválida do dispositivo Arduino: " + receivedMessage);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Não é necessário neste caso
                }
            });

            // Limpe o texto após 5 segundos
            new Handler().postDelayed(() -> textViewStatus.setText(""), 5000);

            updateStatusText("Conectado ao servidor MQTT (Arduino).");
        } catch (Exception e) {
            updateStatusText("Falha ao conectar ao servidor MQTT (Arduino).");
            e.printStackTrace();
        }
    }



    private void aumentarTemperatura() {
        double currentValue = halfGauge.getValue();
        double newValue = currentValue + 5.0;
        if (newValue > halfGauge.getMaxValue()) {
            newValue = halfGauge.getMaxValue();
        }
        halfGauge.setValue(newValue);
    }

    private void diminuirTemperatura() {
        double currentValue = halfGauge.getValue();
        double newValue = currentValue - 5.0;
        if (newValue < halfGauge.getMinValue()) {
            newValue = halfGauge.getMinValue();
        }
        halfGauge.setValue(newValue);
    }

    private void publishToAndroid() {
        try {
            if (androidClient != null && androidClient.isConnected()) {
                String message = "APP_MSG:" + halfGauge.getValue();
                androidClient.publish(ANDROID_MQTT_TOPIC, message.getBytes(), 0, false);
                updateStatusText("Temperatura enviada para o servidor MQTT (Android): " + halfGauge.getValue());
                // Exibe a mensagem por 5 segundos
                showToast("Temperatura enviada para o servidor MQTT (Android): " + halfGauge.getValue(), 5000);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private double lastSentTemperature = 0.0;

    private void publishToArduino() {
        try {
            if (arduinoClient != null && arduinoClient.isConnected()) {
                String message = "APP_MSG: " + halfGauge.getValue();
                arduinoClient.publish(ARDUINO_MQTT_TOPIC, message.getBytes(), 0, false);
                lastSentTemperature = halfGauge.getValue(); // Armazena a última temperatura enviada
                updateStatusText("Temperatura enviada para o dispositivo Arduino: " + lastSentTemperature, 5000);
                // Exibe a mensagem por 5 segundos
                //showToast("Temperatura enviada para o dispositivo Arduino: " + lastSentTemperature, 5000);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void updateStatusText(String message, int duration) {
        runOnUiThread(() -> {
            textViewStatus.setText(message);
            // Define um atraso antes de limpar o texto
            new Handler().postDelayed(() -> textViewStatus.setText(""), duration);
        });
    }



    private void subscribeToAndroidMqttTopic() {
        try {
            if (androidClient != null && androidClient.isConnected()) {
                androidClient.subscribe(ANDROID_MQTT_TOPIC, 0);
            }
        } catch (MqttException e) {
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
            if (androidClient != null && androidClient.isConnected()) {
                androidClient.disconnect();
            }
            if (arduinoClient != null && arduinoClient.isConnected()) {
                arduinoClient.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
