package com.onlytl.mqtt.spring.boot.starter.handler;


import org.springframework.messaging.Message;

/**
 * <p>
 * MqttMessageHandler
 * </p >
 *
 * @author TL
 * @version 1.0.0
 */
public interface MqttMessageHandler {
    /**
     * 处理MQTT消息
     *
     * @param message 消息
     * @param topic 主题
     * @param clientName 客户端名称
     */
    void handleMessage(Message<?> message, String topic, String clientName);
}
