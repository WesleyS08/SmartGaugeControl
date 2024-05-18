#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <WiFi.h>
#include <DHT.h>
#include <PubSubClient.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

#define TEMPOLEITURA 100
#define TEMPOESPERA 300
#define INTERVALO_LEITURA 600
#define MAX_CONSECUTIVE_FAILURES 3

const char* ssid = "LIVE TIM_0C19_2G";
const char* password = "2f5r2pmppenr7u46";
const char* mqtt_server = "public.mqtthq.com";
const char* mqtt_username = "";
const char* mqtt_password = "";
const char* clientID = "ESP32Client";

const int ledPin = 2;
const int ledPin2 = 18;
byte consecutiveFailures = 0;
LiquidCrystal_I2C lcd(0x27, 16, 2);

#define DHTPIN 4
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

WiFiClient espClient;
PubSubClient client(espClient);

unsigned long previousMillisTest = 0;
unsigned long intervalLED = 500;
unsigned long previousMillisLED = 0;
unsigned long previousMillisTemp = 0;  // Variável para controlar o intervalo entre as medições de temperatura

String getTimeFromGoogle() {
  HTTPClient http;

  // Faz uma requisição GET para a API do Google Time
  http.begin("http://worldtimeapi.org/api/ip");

  int httpResponseCode = http.GET();

  String time;

  if (httpResponseCode > 0) {
    // Se a requisição foi bem sucedida, lê a resposta JSON
    DynamicJsonDocument doc(1024);
    DeserializationError error = deserializeJson(doc, http.getStream());

    if (!error) {
      // Obtém o horário atual do JSON retornado
      const char* currentTime = doc["datetime"];
      // Extrai apenas a hora
      String hour = String(currentTime).substring(11, 19);
      time = hour;
    }
  }

  http.end();  // Fecha a conexão

  return time;
}

void setup_wifi() {
  delay(10);
  Serial.println("Conectando ao WiFi...");
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Conectando ao");
  lcd.setCursor(3, 1);
  lcd.print("WiFi.......");

  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    digitalWrite(ledPin2, HIGH);
    delay(500);
    digitalWrite(ledPin2, LOW);
    delay(500);
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\nWiFi conectado!");
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("WiFi Conectado");
    lcd.setCursor(0, 1);
    lcd.print("IP: ");
    lcd.print(WiFi.localIP());
    delay(5000);
    lcd.clear();
    consecutiveFailures = 0;
  } else {
    Serial.println("\nFalha ao conectar ao WiFi!");
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Falha ao conectar");
    lcd.setCursor(0, 1);
    lcd.print("ao WiFi!");
    delay(5000);
    lcd.clear();
    consecutiveFailures++;
    if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
      lcd.setCursor(0, 0);
      lcd.print("Reiniciando ESP32...");
      delay(10000);
      ESP.restart();
    }
  }
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Tentando conectar");
  lcd.setCursor(0, 1);
  lcd.print("ao  MQTT!");
  delay(5000);
  lcd.clear();
}

void callback(char* topic, byte* payload, unsigned int length) {
  // Converte o payload em uma string
  String message;
  for (int i = 0; i < length; i++) {
    message += (char)payload[i];
  }

  // Verifica se a mensagem recebida corresponde ao conteúdo desejado
  if (message.equals("Reiniciar o Sistema!!!")) {
    digitalWrite(ledPin2, HIGH);
    delay(500);
    digitalWrite(ledPin2, LOW);
    delay(500);
    Serial.println("Mensagem recebida: Reiniciar o Sistema!!!");
    lcd.clear();               // Limpa o LCD
    lcd.setCursor(0, 0);       // Define o cursor para a primeira linha
    lcd.print("Reiniciar o");  // Exibe a mensagem no LCD
    lcd.setCursor(0, 1);       // Define o cursor para a segunda linha
    lcd.print("Sistema!!!");   // Exibe a mensagem no LCD
    delay(500);                // Aguarda por um tempo para visualização
    lcd.clear();
    ESP.restart();  // Reinicia o Arduino
  }
}

void reconnect() {
  unsigned long currentMillis = millis();
  if (currentMillis - previousMillisLED >= intervalLED) {
    previousMillisLED = currentMillis;
    digitalWrite(ledPin, !digitalRead(ledPin));
  }

  while (!client.connected()) {
    Serial.println("Tentando reconectar ao MQTT...");
    if (client.connect(clientID, mqtt_username, mqtt_password)) {
      Serial.println("Conectado ao servidor MQTT");
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print("Conectado MQTT");
      lcd.setCursor(0, 1);
      lcd.print("Tirando a Temp...");
      client.subscribe("temperatura");
      digitalWrite(ledPin, HIGH);
    } else {
      Serial.print("Falha na conexão, rc=");
      Serial.print(client.state());
      Serial.println(" Tentando novamente em 5 segundos...");
      delay(5000);
    }
  }
}

void updateTemperatureAndPublish() {
  unsigned long currentMillis = millis();
  if (currentMillis - previousMillisTemp >= INTERVALO_LEITURA) {
    previousMillisTemp = currentMillis;

    float temperatura = dht.readTemperature();

    if (isnan(temperatura)) {
      Serial.println("Falha ao ler o sensor DHT!");
      lcd.setCursor(0, 1);
      lcd.print("Erro no sensor");
      digitalWrite(ledPin2, HIGH);

    } else {
      Serial.print("Temperatura: ");
      Serial.print(temperatura);
      Serial.println(" *C");

      lcd.setCursor(0, 1);
      lcd.print("Temp: ");
      lcd.print(temperatura);
      lcd.print(" C...");

      // Obter o horário atual do Google
      String currentTime = getTimeFromGoogle();

      // Montar a mensagem com a temperatura e o horário
      String message = "Temperatura: " + String(temperatura) + " C, Hora: " + currentTime;

      // Enviar a mensagem para o MQTT
      client.publish("temperatura", message.c_str());
    }
  }
}

void setup() {
  Serial.begin(115200);
  Wire.begin();

  pinMode(ledPin, OUTPUT);
  pinMode(ledPin2, OUTPUT);
  lcd.init();
  lcd.backlight();

  lcd.setCursor(0, 0);
  lcd.print("Conectando ao");
  lcd.setCursor(4, 1);
  lcd.print("MQTT......");

  setup_wifi();

  client.setServer(mqtt_server, 1883);
  client.setCallback(callback);

  dht.begin();
}

void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  updateTemperatureAndPublish();

  delay(INTERVALO_LEITURA);  // Aguarda o intervalo entre as leituras
}
