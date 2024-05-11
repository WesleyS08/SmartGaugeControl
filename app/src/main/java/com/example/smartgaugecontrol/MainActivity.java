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

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
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
        range1.setColor(Color.parseColor("#0000FF"));
        range1.setFrom(0.0);
        range1.setTo(15.0);

        Range range2 = new Range();
        range2.setColor(Color.parseColor("#008000"));
        range2.setFrom(15.0);
        range2.setTo(25.0);

        Range range3 = new Range();
        range3.setColor(Color.parseColor("#FF0000"));
        range3.setFrom(25.0);
        range3.setTo(40.0);

        halfGauge.addRange(range1);
        halfGauge.addRange(range2);
        halfGauge.addRange(range3);

        halfGauge.setMinValue(0.0);
        halfGauge.setMaxValue(40.0);
        halfGauge.setValue(20.0);

        textViewStatus.setText("Conectando aos servidores MQTT...");

        setupAndroidMqttClient();
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

    private void setupAndroidMqttClient() {
        try {
            String clientId = MqttClient.generateClientId();
            androidClient = new MqttClient("tcp://c023193fa6834fdaa25f0b48c176dd69.s1.eu.hivemq.cloud:8883", clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setUserName("Wesley1.0");
            options.setPassword("Ws58247889!".toCharArray());

            androidClient.connect(options);
            androidClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    textViewStatus.setText("Conexão MQTT (Android) perdida.");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    double newTemperature = Double.parseDouble(new String(message.getPayload()));
                    halfGauge.setValue(newTemperature);
                    updateStatusText("Temperatura recebida do servidor MQTT (Android): " + newTemperature);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not needed in this case
                }
            });
            textViewStatus.setText("Conectado ao servidor MQTT (Android).");
        } catch (Exception e) {
            textViewStatus.setText("Falha ao conectar ao servidor MQTT (Android).");
            e.printStackTrace();
        }
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
                    textViewStatus.append("\nConexão MQTT (Arduino) perdida.");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    textViewStatus.append("\nMensagem recebida do dispositivo Arduino: " + new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not needed in this case
                }
            });
            textViewStatus.append("\nConectado ao servidor MQTT (Arduino).");
        } catch (Exception e) {
            textViewStatus.append("\nFalha ao conectar ao servidor MQTT (Arduino).");
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
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void publishToArduino() {
        try {
            if (arduinoClient != null && arduinoClient.isConnected()) {
                String message = "Temperature: " + halfGauge.getValue();
                arduinoClient.publish(ARDUINO_MQTT_TOPIC, message.getBytes(), 0, false);
                updateStatusText("Temperatura enviada para o dispositivo Arduino: " + halfGauge.getValue());
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
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

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
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
