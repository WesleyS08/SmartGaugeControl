package com.example.smartgaugecontrol;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
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
import java.text.DecimalFormat;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private HalfGauge halfGauge;
    private TextView textViewStatus;

    private MqttClient arduinoClient;
    private static final String ARDUINO_MQTT_TOPIC = "BehYNK2qm%QRo5Wwm@8ouJ"; // Tópico MQTT para o dispositivo Arduino

    private DecimalFormat decimalFormat = new DecimalFormat("#.##"); // Formato para até duas casas decimais

    // Handler para fazer o ponteiro tremer após 2 segundos de inatividade
    private Handler trembleHandler = new Handler(Looper.getMainLooper());
    private Runnable trembleRunnable = new Runnable() {
        @Override
        public void run() {
            trembleGauge();
            // Agendar novamente o tremor após 2 segundos
            startTrembleHandler();
        }
    };

    // Variável para controlar se o usuário interagiu com o medidor
    private boolean userInteracted = false;

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
        double initialValue = setInitialRandomGaugeValue(); // Define um valor inicial aleatório dentro do intervalo permitido
        publishInitialValueToArduino(initialValue); // Publica o valor inicial aleatório no servidor MQTT

        textViewStatus.setText("Conectando ao servidor MQTT...");

        setupArduinoMqttClient();

        // Configurar listeners de clique dos botões
        Button btnAumentar = findViewById(R.id.btnAumentar);
        Button btnDiminuir = findViewById(R.id.btnDiminuir);

        // Configurar listeners de clique dos botões
        btnAumentar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!userInteracted) {
                    double currentValue = halfGauge.getValue();
                    double newValue = currentValue + 5.0;
                    if (newValue > halfGauge.getMaxValue()) {
                        newValue = halfGauge.getMaxValue();
                    }
                    // Atualiza o valor do gráfico e envia para o Arduino
                    updateGaugeValue(newValue);
                    publishToArduino(decimalFormat.format(newValue)); // Formata o valor com até duas casas decimais
                    // Reinicia o handler para fazer o ponteiro tremer
                    restartTrembleHandler();
                }
            }
        });

        btnDiminuir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!userInteracted) {
                    double currentValue = halfGauge.getValue();
                    double newValue = currentValue - 5.0;
                    if (newValue < halfGauge.getMinValue()) {
                        newValue = halfGauge.getMinValue();
                    }
                    // Atualiza o valor do gráfico e envia para o Arduino
                    updateGaugeValue(newValue);
                    publishToArduino(decimalFormat.format(newValue)); // Formata o valor com até duas casas decimais
                    // Reinicia o handler para fazer o ponteiro tremer
                    restartTrembleHandler();
                }
            }
        });

        // Inicia o handler para fazer o ponteiro tremer após 2 segundos de inatividade
        startTrembleHandler();

        // Configura um listener de toque para o medidor
        halfGauge.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Usuário interagiu com o medidor
                        userInteracted = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        // Usuário parou de interagir com o medidor
                        userInteracted = false;
                        // Reinicia o handler para fazer o ponteiro tremer após 2 segundos de inatividade
                        restartTrembleHandler();
                        break;
                }
                return false;
            }
        });
    }

    private void startTrembleHandler() {
        trembleHandler.postDelayed(trembleRunnable, 2000); // 2 segundos
    }

    private void restartTrembleHandler() {
        trembleHandler.removeCallbacks(trembleRunnable);
        startTrembleHandler();
    }

    // Método para fazer o ponteiro tremer
    private void trembleGauge() {
        double currentValue = halfGauge.getValue();
        Random random = new Random();

        // Definir os limites para o tremor com base na posição atual do ponteiro
        double minValue = currentValue - 3; // Limite inferior é 3 unidades abaixo da posição atual
        double maxValue = currentValue + 3; // Limite superior é 3 unidades acima da posição atual

        // Limitar os limites dentro do intervalo permitido (-40 a 80)
        minValue = Math.max(minValue, halfGauge.getMinValue());
        maxValue = Math.min(maxValue, halfGauge.getMaxValue());

        // Gera um valor aleatório dentro do intervalo definido
        double newValue = random.nextDouble() * (maxValue - minValue) + minValue;

        // Atualiza o valor do medidor com o novo valor
        updateGaugeValue(newValue);

        // Envia o novo valor para o servidor MQTT
        publishToArduino(String.format("%.2f", newValue)); // Formata o valor com até duas casas decimais
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

                    // Verificar se a mensagem contém "APP_ARD"
                    if (receivedMessage.contains("APP_ARD")) {
                        // Ajustar a temperatura com base no número recebido
                        try {
                            // Extrair o valor numérico da temperatura da mensagem
                            String[] parts = receivedMessage.split(":");
                            if (parts.length == 2) {
                                double receivedTemperature = Double.parseDouble(parts[1].trim());
                                updateGaugeValue(receivedTemperature);
                                //updateStatusText("Temperatura ajustada: " + receivedTemperature);
                            } else {
                                updateStatusText("Mensagem recebida com formato inválido: " + receivedMessage);
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            updateStatusText("Falha ao converter temperatura: " + receivedMessage);
                        }
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

    // Método para definir um valor inicial aleatório dentro do intervalo permitido (-40 a 80) com até duas casas decimais
    private double setInitialRandomGaugeValue() {
        Random random = new Random();
        double randomValue = Math.round((random.nextDouble() * (80.0 - (-40.0)) + (-40.0)) * 100.0) / 100.0; // Gera um valor aleatório entre -40 e 80 com até duas casas decimais
        updateGaugeValue(randomValue);
        return randomValue;
    }

    // Método para publicar o valor inicial aleatório no servidor MQTT
    private void publishInitialValueToArduino(double value) {
        String formattedValue = decimalFormat.format(value);
        publishToArduino(formattedValue);
    }

    // Atualiza o valor do TextView
    private void updateGaugeValue(double value) {
        double formattedValue = Double.parseDouble(decimalFormat.format(value)); // Formata o valor com até duas casas decimais
        halfGauge.setValue(formattedValue);
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

    private void publishToArduino(String value) {
        try {
            if (arduinoClient != null && arduinoClient.isConnected()) {
                String message = value;
                arduinoClient.publish(ARDUINO_MQTT_TOPIC, message.getBytes(), 0, false);
                //updateStatusText("Mensagem enviada para o Arduino: " + value);

                // Limpa o texto após 5 segundos
                new Handler(Looper.getMainLooper()).postDelayed(() -> updateStatusText(""), 5000);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
