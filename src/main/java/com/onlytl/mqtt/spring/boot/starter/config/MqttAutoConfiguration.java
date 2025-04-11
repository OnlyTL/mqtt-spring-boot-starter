package com.onlytl.mqtt.spring.boot.starter.config;

import com.onlytl.mqtt.spring.boot.starter.factory.MqttClientFactory;
import com.onlytl.mqtt.spring.boot.starter.factory.MqttPahoClientFactoryImpl;
import com.onlytl.mqtt.spring.boot.starter.handler.DefaultMqttMessageHandler;
import com.onlytl.mqtt.spring.boot.starter.handler.MqttMessageHandler;
import com.onlytl.mqtt.spring.boot.starter.template.MqttTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * <p>
 * MqttAutoConfiguration
 * </p >
 *
 * @author TL
 * @version 1.0.0
 */
@Configuration
@EnableConfigurationProperties(MqttProperties.class)
@ConditionalOnProperty(prefix = "mqtt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MqttAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MqttClientFactory mqttClientFactory() {
        return new MqttPahoClientFactoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public MqttMessageHandler mqttMessageHandler() {
        return new DefaultMqttMessageHandler();
    }

    @Bean
    public MqttClientConfiguration mqttClientConfiguration(MqttProperties mqttProperties,
                                                           MqttClientFactory mqttClientFactory,
                                                           MqttMessageHandler mqttMessageHandler,
                                                           ThreadPoolTaskScheduler mqttTaskScheduler) { // 注入调度器
        return new MqttClientConfiguration(mqttProperties, mqttClientFactory, mqttMessageHandler);
    }

    @Bean
    public MqttTemplate mqttTemplate(MqttClientConfiguration mqttClientConfiguration) {
        return new MqttTemplate(
                mqttClientConfiguration.getOutboundHandlers(),
                mqttClientConfiguration.getClientConfigs());
    }
}
