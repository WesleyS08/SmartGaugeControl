#include <WiFi.h>
#include <Wire.h>
#include <ArduinoJson.h>
#include <PubSubClient.h>
#include <LiquidCrystal_I2C.h>
#include <DHT.h>

// Parâmetros do servidor MQTT
#define MQTT_CLIENT_ID "micropython-weather-demo"
#define MQTT_BROKER    "public.mqtthq.com"
#define MQTT_USER      ""
#define MQTT_PASSWORD  ""
#define MQTT_TOPIC     "BehYNK2qm%QRo5Wwm@8ouJ"

// Pinos do LCD
#define SDA_PIN        21
#define SCL_PIN        22
#define DHT_PIN        15

// Pinos dos LEDs
#define LED_PIN_WIFI   25 // LED para indicar conexão WiFi
#define LED_PIN_MQTT   26 // LED para indicar conexão MQTT
#define LED_PIN_TEMP_1 27 // LED para faixa de temperatura -40 a 0
#define LED_PIN_TEMP_2 32 // LED para faixa de temperatura 0 a 40
#define LED_PIN_TEMP_3 33 // LED para faixa de temperatura 40 a 80

WiFiClient espClient;
PubSubClient client(espClient);

LiquidCrystal_I2C lcd(0x27, 16, 2); // Endereço I2C e tamanho do LCD
DHT dht(DHT_PIN, DHT22);

float currentTemperature = 0.0; // Variável para armazenar a temperatura atual do sensor

void callback(char* topic, byte* payload, unsigned int length) {
  // Convertendo a carga útil (payload) para uma string
  String message = "";
  for (int i = 0; i < length; i++) {
    message += (char)payload[i];
  }

  // Verificar se a mensagem contém um valor numérico
  float receivedTemperature = message.toFloat();
  if (!isnan(receivedTemperature)) {
    currentTemperature = receivedTemperature; // Define a temperatura atual como o valor recebido
    Serial.print("Temperatura recebida: ");
    Serial.println(currentTemperature);
    
    // Atualizar os LEDs de acordo com as faixas de temperatura
    updateTemperatureLEDs(currentTemperature);
  } else {
    Serial.println("Mensagem MQTT recebida, mas não contém um valor numérico válido.");
  }
}

void setup() {
  Serial.begin(115200);

  // Inicialização dos LEDs
  pinMode(LED_PIN_WIFI, OUTPUT);
  pinMode(LED_PIN_MQTT, OUTPUT);
  pinMode(LED_PIN_TEMP_1, OUTPUT);
  pinMode(LED_PIN_TEMP_2, OUTPUT);
  pinMode(LED_PIN_TEMP_3, OUTPUT);

  // Inicialização do LCD
  lcd.init();
  lcd.backlight();
  lcd.setCursor(0, 0);
  lcd.print("Conectando");
  lcd.setCursor(0, 1);
  lcd.print("wi-fi ....");

  WiFi.begin("Wokwi-GUEST", "");
  while (WiFi.status() != WL_CONNECTED) {
    digitalWrite(LED_PIN_WIFI, !digitalRead(LED_PIN_WIFI)); // Pisca o LED WiFi
    delay(500);
    lcd.print(".");
    digitalWrite(LED_PIN_WIFI, !digitalRead(LED_PIN_WIFI)); // Pisca o LED WiFi
    delay(500);
  }

  digitalWrite(LED_PIN_WIFI, HIGH); // LED WiFi fica aceso quando conectado

  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Conectando");
  lcd.setCursor(0, 1);
  lcd.print("MQTT ....");

  // Configura o servidor MQTT e a porta
  client.setServer(MQTT_BROKER, 1883); 
  client.setCallback(callback);

  // Tenta se conectar ao servidor MQTT
  if (!client.connected()) {
    reconnect();
  }
}

void loop() {
  client.loop();
}

void reconnect() {
  while (!client.connected()) {
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Tentando conectar");
     lcd.setCursor(0, 1);
    lcd.print("Ao MQTT...");

    // Tenta conectar
    if (client.connect(MQTT_CLIENT_ID, MQTT_USER, MQTT_PASSWORD)) {
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print("Conectado");
      digitalWrite(LED_PIN_MQTT, HIGH); // LED MQTT fica aceso quando conectado
      client.subscribe(MQTT_TOPIC);
    } else {
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print("Falha na conexão MQTT");
      digitalWrite(LED_PIN_MQTT, !digitalRead(LED_PIN_MQTT)); // Pisca o LED MQTT
      delay(1000);
    }
  }
}

void updateTemperatureLEDs(float temperature) {
  // Desligar todos os LEDs de temperatura
  digitalWrite(LED_PIN_TEMP_1, LOW);
  digitalWrite(LED_PIN_TEMP_2, LOW);
  digitalWrite(LED_PIN_TEMP_3, LOW);

  // Atualizar os LEDs com base nas faixas de temperatura
  if (temperature < 0) {
    digitalWrite(LED_PIN_TEMP_1, HIGH); // LED para a faixa de temperatura -40 a 0
  } else if (temperature >= 0 && temperature <= 40) {
    digitalWrite(LED_PIN_TEMP_2, HIGH); // LED para a faixa de temperatura 0 a 40
  } else if (temperature > 40 && temperature <= 80) {
    digitalWrite(LED_PIN_TEMP_3, HIGH); // LED para a faixa de temperatura 40 a 80
  }
}
