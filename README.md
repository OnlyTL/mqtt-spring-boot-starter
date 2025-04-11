# MQTT Spring Boot Starter

---

[![English](https://img.shields.io/badge/Language-English-blue)](README.md) [![中文](https://img.shields.io/badge/Language-中文-red)](docs/README-zh.md)


A comprehensive Spring Boot starter for MQTT communication with SSL support and multi-client capabilities.

## Features

- **Annotation-based subscriptions**: Easily subscribe to MQTT topics using annotations
- **Multiple broker support**: Connect to multiple MQTT brokers within the same application
- **SSL/TLS support**: Secure communication with MQTT brokers using various certificate formats
- **Flexible message handling**: Process MQTT messages with customizable handlers
- **Topic wildcards**: Full support for MQTT topic wildcards (`+` and `#`)
- **Auto-reconnect**: Automatic reconnection handling
- **QoS control**: Configurable Quality of Service levels

## Installation

Add the following dependency to your Maven project:

```xml
<dependency>
    <groupId>com.onlytl</groupId>
    <artifactId>mqtt-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### 1. Enable MQTT support in your Spring Boot application

```java
@SpringBootApplication
@EnableMqttClients
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 2. Configure MQTT properties in application.yml

```yaml
mqtt:
  enabled: true
  thread-pool:
    size: 10
  default-client:
    server-uri: tcp://localhost:1883  # or ssl://localhost:8883 for SSL
    client-id: client-${random.uuid}
    username: your-username  # optional
    password: your-password  # optional
    default-topic: default/topic
    default-qos: 1
```

### 3. Create a message handler

```java
@Component
@MqttClient(name = "default")
public class MqttMessageHandler {

    @MqttSubscribe("topic/test")
    public void handleTemperature(String payload) {
        System.out.println("test received: " + payload);
    }
    
    @MqttSubscribe(topic = "server/+/status", qos = 2)
    public void handleDeviceStatus(String payload, String topic) {
        System.out.println("server status from topic " + topic + ": " + payload);
    }
}
```

### 4. Send messages

```java
@Service
public class ServerService {

    @Autowired
    private MqttTemplate mqttTemplate;
    
    public void sendCommand(String deviceId, String command) {
        mqttTemplate.send(command, "server/" + deviceId + "/command", "default");
    }
    
    public void sendToDefaultTopic(String message) {
        mqttTemplate.sendToDefaultTopic(message, "default");
    }
}
```

## Advanced Configuration

### SSL/TLS Configuration

To enable SSL/TLS support, use the following configuration:

```yaml
mqtt:
  default-client:
    server-uri: ssl://mqtt-broker:8883
    ssl:
      enabled: true
      cert-type: PEM  # or JKS
      ca-file: classpath:cert/ca.pem
      client-cert-file: classpath:cert/client.pem
      client-key-file: classpath:cert/client.key
      # client-key-password: optional-password
```

For Java KeyStore format:

```yaml
mqtt:
  default-client:
    server-uri: ssl://mqtt-broker:8883
    ssl:
      enabled: true
      cert-type: JKS
      trust-store: classpath:cert/truststore.jks
      trust-store-password: password
      key-store: classpath:cert/keystore.jks
      key-store-password: password
```

### Multiple Brokers Configuration

```yaml
mqtt:
  default-client:
    server-uri: tcp://main-broker:1883
    # other properties...
  clients:
    secondary:
      server-uri: tcp://secondary-broker:1883
      username: another-user
      password: another-password
      default-topic: secondary/topic
    monitoring:
      server-uri: ssl://monitoring-broker:8883
      ssl:
        enabled: true
        # SSL properties...
```

To subscribe to topics from a specific broker:

```java
@MqttSubscribe(topic = "data/logs", client = "secondary")
public void handleSecondaryLogs(String payload) {
    // Process logs from secondary broker
}

@MqttSubscribe(topic = "monitoring/alerts", client = "monitoring")
public void handleAlerts(String payload) {
    // Process alerts from monitoring broker
}
```

### Thread Pool Configuration

You can customize the MQTT task scheduler thread pool:

```yaml
mqtt:
  thread-pool:
    size: 20  # Number of threads
    name-prefix: mqtt-worker-  # Thread name prefix
    wait-for-tasks-to-complete-on-shutdown: true
    await-termination-seconds: 60
```

## Message Handling

### Parameter Types

You can define your subscription methods with different parameter types:

```java
// Single payload parameter
@MqttSubscribe("topic/one")
public void handle1(String payload) {
    // Process string payload
}

// Payload and topic
@MqttSubscribe("topic/two")
public void handle2(String payload, String topic) {
    // Process payload with topic information
}

// Payload, topic, and client name
@MqttSubscribe("topic/three")
public void handle3(String payload, String topic, String clientName) {
    // Process with client information
}

// Binary data (original payload)
@MqttSubscribe("topic/binary")
public void handleBinary(byte[] payload) {
    // Process binary data
}
```

### Custom Message Handler

You can create a custom global message handler:

```java
@Bean
@Primary
public MqttMessageHandler customMessageHandler() {
    return new MqttMessageHandler() {
        @Override
        public void handleMessage(Message<?> message, String topic, String clientName) {
            // Custom global message handling logic
            System.out.println("Custom handler received message on topic: " + topic);
        }
    };
}
```

## Topic Wildcards

MQTT supports two wildcard characters for topic subscriptions:

- `+`: Matches exactly one level
- `#`: Matches any number of levels (must be the last character)

Examples:

```java
@MqttSubscribe("device/+/status")
public void handleAnyDeviceStatus(String payload, String topic) {
    // Matches: device/device1/status, device/device2/status, etc.
}

@MqttSubscribe("sensor/#")
public void handleAllSensorData(String payload, String topic) {
    // Matches: sensor/temp, sensor/humidity, sensor/location/gps, etc.
}
```

## Error Handling

The starter includes built-in error handling for message processing. All exceptions are caught and logged, preventing message processing failures from affecting the connection.

For custom error handling:

```java
@Component
public class MqttErrorHandler implements ErrorHandler {
    @Override
    public void handleError(Throwable t) {
        // Custom error handling logic
    }
}
```

## Building from Source

```bash
git clone https://github.com/yourusername/mqtt-spring-boot-starter.git
cd mqtt-spring-boot-starter
mvn clean install
```

## Dependencies

- Spring Boot 2.x or higher
- Spring Integration MQTT
- Eclipse Paho MQTT Client
- BouncyCastle (for SSL support)

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.