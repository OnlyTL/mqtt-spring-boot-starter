package com.onlytl.mqtt.spring.boot.starter.template;


import com.onlytl.mqtt.spring.boot.starter.config.MqttProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * <p>
 * MqttTemplate
 * </p >
 *
 * @author TL
 * @version 1.0.0
 */
@Slf4j
public class MqttTemplate {

    private final Map<String, MqttPahoMessageHandler> messageHandlers;
    private final Map<String, MqttProperties.ClientConfig> clientConfigs;

    public MqttTemplate(Map<String, MqttPahoMessageHandler> messageHandlers,
                        Map<String, MqttProperties.ClientConfig> clientConfigs) {
        this.messageHandlers = messageHandlers;
        this.clientConfigs = clientConfigs;
    }

    /**
     * 发送消息到默认主题
     *
     * @param payload 消息内容
     * @param clientName 客户端名称
     */
    public void sendToDefaultTopic(Object payload, String clientName) {
        MqttProperties.ClientConfig config = getClientConfig(clientName);
        Assert.hasText(config.getDefaultTopic(),
                "Default topic not configured for client: " + clientName);

        send(payload, config.getDefaultTopic(), config.getDefaultQos(), clientName);
    }

    /**
     * 发送消息到指定主题
     *
     * @param payload 消息内容
     * @param topic 主题
     * @param clientName 客户端名称
     */
    public void send(Object payload, String topic, String clientName) {
        MqttProperties.ClientConfig config = getClientConfig(clientName);
        send(payload, topic, config.getDefaultQos(), clientName);
    }

    /**
     * 发送消息到指定主题，并指定QoS
     *
     * @param payload 消息内容
     * @param topic 主题
     * @param qos QoS等级
     * @param clientName 客户端名称
     */
    public void send(Object payload, String topic, int qos, String clientName) {
        MqttPahoMessageHandler messageHandler = messageHandlers.get(clientName);
        if (messageHandler == null) {
            throw new IllegalStateException("No MQTT client found with name: " + clientName);
        }

        Message<?> message = MessageBuilder.withPayload(payload)
                .setHeader(MqttHeaders.TOPIC, topic)
                .setHeader(MqttHeaders.QOS, qos)
                .build();

        try {
            messageHandler.handleMessage(message);
            log.debug("Sent message to topic [{}] with client [{}]", topic, clientName);
        } catch (MessagingException e) {
            log.error("Failed to send message to topic [{}] with client [{}]", topic, clientName, e);
            throw e;
        }
    }

    private MqttProperties.ClientConfig getClientConfig(String clientName) {
        MqttProperties.ClientConfig config = clientConfigs.get(clientName);
        if (config == null) {
            throw new IllegalStateException("No MQTT client configuration found with name: " + clientName);
        }
        return config;
    }
}
