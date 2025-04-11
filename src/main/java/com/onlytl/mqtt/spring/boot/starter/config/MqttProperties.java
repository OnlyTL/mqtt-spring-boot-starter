package com.onlytl.mqtt.spring.boot.starter.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * MqttProperties
 * </p >
 *
 * @author TL
 * @version 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {
    /**
     * 是否启用MQTT
     */
    private boolean enabled = true;

    /**
     * 线程池配置
     */
    @NestedConfigurationProperty
    private MqttSchedulerConfig threadPool = new MqttSchedulerConfig();

    /**
     * 默认客户端配置
     */
    @NestedConfigurationProperty
    private ClientConfig defaultClient = new ClientConfig();

    /**
     * 多客户端配置，key为客户端名称
     */
    private Map<String, ClientConfig> clients = new HashMap<>();

    @Data
    public static class ClientConfig {
        /**
         * MQTT服务器地址，例如：tcp://localhost:1883或ssl://localhost:8883
         */
        private String serverUri = "tcp://localhost:1883";

        /**
         * 客户端ID
         */
        private String clientId = "mqtt-client-" + System.currentTimeMillis();

        /**
         * 用户名
         */
        private String username;

        /**
         * 密码
         */
        private String password;

        /**
         * 清除会话
         */
        private boolean cleanSession = true;

        /**
         * 连接超时时间（秒）
         */
        private int connectionTimeout = 30;

        /**
         * 保持连接心跳时间（秒）
         */
        private int keepAliveInterval = 60;

        /**
         * 是否自动重连
         */
        private boolean automaticReconnect = true;

        /**
         * 默认的QoS级别
         */
        private int defaultQos = 1;

        /**
         * 默认主题
         */
        private String defaultTopic;

        /**
         * SSL配置
         */
        @NestedConfigurationProperty
        private SslProperties ssl = new SslProperties();
    }
}
