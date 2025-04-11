# MQTT Spring Boot Starter（MQTT 启动器）

---
[![English](https://img.shields.io/badge/Language-English-blue)](../README.md) [![中文](https://img.shields.io/badge/Language-中文-red)](README-zh.md)


一个支持 SSL 和多客户端的全面 MQTT 通信 Spring Boot 启动器。

## 功能特色

- **基于注解的订阅**：使用注解轻松订阅 MQTT 主题
- **多 Broker 支持**：在同一个应用中连接多个 MQTT Broker
- **SSL/TLS 支持**：使用多种证书格式实现安全通信
- **灵活的消息处理**：使用可自定义的处理器处理 MQTT 消息
- **主题通配符支持**：完全支持 MQTT 的 `+` 和 `#` 通配符
- **自动重连**：断线自动重连
- **QoS 控制**：支持可配置的服务质量（QoS）级别

## 安装

在你的 Maven 项目中添加以下依赖：

```xml
<dependency>
    <groupId>com.onlytl</groupId>
    <artifactId>mqtt-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 快速开始

### 1. 启用 MQTT 支持

```java
@SpringBootApplication
@EnableMqttClients
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 2. 在 `application.yml` 中配置 MQTT 属性

```yaml
mqtt:
  enabled: true
  thread-pool:
    size: 10
  default-client:
    server-uri: tcp://localhost:1883  # SSL 使用 ssl://localhost:8883
    client-id: client-${random.uuid}
    username: your-username  # 可选
    password: your-password  # 可选
    default-topic: default/topic
    default-qos: 1
```

### 3. 创建消息处理器

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

### 4. 发送消息

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

## 高级配置

### SSL/TLS 配置

启用 SSL/TLS：

```yaml
mqtt:
  default-client:
    server-uri: ssl://mqtt-broker:8883
    ssl:
      enabled: true
      cert-type: PEM  # 或 JKS
      ca-file: classpath:cert/ca.pem
      client-cert-file: classpath:cert/client.pem
      client-key-file: classpath:cert/client.key
      # client-key-password: 可选密码
```

使用 Java KeyStore 格式：

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

### 多个 Broker 配置

```yaml
mqtt:
  default-client:
    server-uri: tcp://main-broker:1883
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
        # SSL 属性...
```

订阅不同客户端的主题：

```java
@MqttSubscribe(topic = "data/logs", client = "secondary")
public void handleSecondaryLogs(String payload) {
    // 处理 secondary broker 的日志
}

@MqttSubscribe(topic = "monitoring/alerts", client = "monitoring")
public void handleAlerts(String payload) {
    // 处理 monitoring broker 的警报信息
}
```

### 线程池配置

```yaml
mqtt:
  thread-pool:
    size: 20  # 线程数
    name-prefix: mqtt-worker-  # 线程名前缀
    wait-for-tasks-to-complete-on-shutdown: true
    await-termination-seconds: 60
```

## 消息处理

### 参数类型支持

```java
// 只有 payload
@MqttSubscribe("topic/one")
public void handle1(String payload) {
    // 处理字符串内容
}

// payload + topic
@MqttSubscribe("topic/two")
public void handle2(String payload, String topic) {
    // 带有主题信息的处理
}

// payload + topic + client 名称
@MqttSubscribe("topic/three")
public void handle3(String payload, String topic, String clientName) {
    // 处理特定客户端的消息
}

// 原始二进制数据
@MqttSubscribe("topic/binary")
public void handleBinary(byte[] payload) {
    // 处理二进制数据
}
```

### 自定义全局消息处理器

```java
@Bean
@Primary
public MqttMessageHandler customMessageHandler() {
    return new MqttMessageHandler() {
        @Override
        public void handleMessage(Message<?> message, String topic, String clientName) {
            System.out.println("自定义处理器接收到消息，主题：" + topic);
        }
    };
}
```

## 主题通配符

MQTT 支持以下通配符：

- `+`：匹配一个层级
- `#`：匹配多个层级（必须是最后一个）

示例：

```java
@MqttSubscribe("device/+/status")
public void handleAnyDeviceStatus(String payload, String topic) {
    // 匹配：device/device1/status、device/device2/status 等
}

@MqttSubscribe("sensor/#")
public void handleAllSensorData(String payload, String topic) {
    // 匹配：sensor/temp、sensor/humidity、sensor/location/gps 等
}
```

## 错误处理

内置了消息处理异常捕获机制，避免连接被异常中断。

自定义错误处理：

```java
@Component
public class MqttErrorHandler implements ErrorHandler {
    @Override
    public void handleError(Throwable t) {
        // 自定义错误处理逻辑
    }
}
```

## 从源码构建

```bash
git clone https://github.com/yourusername/mqtt-spring-boot-starter.git
cd mqtt-spring-boot-starter
mvn clean install
```

## 依赖

- Spring Boot 2.x 或以上版本
- Spring Integration MQTT
- Eclipse Paho MQTT 客户端
- BouncyCastle（用于 SSL 支持）

## 许可证

本项目基于 MIT 许可证开源，详情见 LICENSE 文件。

## 贡献

欢迎贡献！欢迎通过 Pull Request 提交你的修改。