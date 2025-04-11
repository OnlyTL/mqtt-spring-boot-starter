package com.onlytl.mqtt.spring.boot.starter.handler;


import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;

/**
 * <p>
 * DefaultMqttMessageHandler
 * </p >
 *
 * @author TL
 * @version 1.0.0
 */
@Slf4j
public class DefaultMqttMessageHandler implements MqttMessageHandler {

    @Override
    public void handleMessage(Message<?> message, String topic, String clientName) {
//        logger.info("Received message from client [{}] on topic [{}]: {}",
//                clientName, topic, message.getPayload());
    }
}
