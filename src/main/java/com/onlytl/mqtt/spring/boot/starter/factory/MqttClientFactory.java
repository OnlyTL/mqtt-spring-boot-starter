package com.onlytl.mqtt.spring.boot.starter.factory;


import com.onlytl.mqtt.spring.boot.starter.config.MqttProperties;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;

/**
 * <p>
 * MqttClientFactory
 * </p >
 *
 * @author TL
 * @version 1.0.0
 */
public interface MqttClientFactory {

    /**
     * 创建MQTT客户端工厂
     *
     * @param clientConfig 客户端配置
     * @return MQTT客户端工厂
     */
    MqttPahoClientFactory createClientFactory(MqttProperties.ClientConfig clientConfig) throws Exception;
}
