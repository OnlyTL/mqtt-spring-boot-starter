package com.onlytl.mqtt.spring.boot.starter.factory;


import com.onlytl.mqtt.spring.boot.starter.config.MqttProperties;
import com.onlytl.mqtt.spring.boot.starter.ssl.SslContextBuilder;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;

import javax.net.ssl.SSLContext;

/**
 * <p>
 * MqttPahoClientFactoryImpl
 * </p >
 *
 * @author TL
 * @version 1.0.0
 */
public class MqttPahoClientFactoryImpl implements MqttClientFactory {

    @Override
    public MqttPahoClientFactory createClientFactory(MqttProperties.ClientConfig clientConfig) throws Exception {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();

        // 设置基本连接属性
        options.setServerURIs(new String[]{clientConfig.getServerUri()});
        if (clientConfig.getUsername() != null) {
            options.setUserName(clientConfig.getUsername());
        }
        if (clientConfig.getPassword() != null) {
            options.setPassword(clientConfig.getPassword().toCharArray());
        }
        options.setCleanSession(clientConfig.isCleanSession());
        options.setConnectionTimeout(clientConfig.getConnectionTimeout());
        options.setKeepAliveInterval(clientConfig.getKeepAliveInterval());
        options.setAutomaticReconnect(clientConfig.isAutomaticReconnect());

        // 配置SSL（如果启用）
        if (clientConfig.getSsl().isEnabled()) {
            SSLContext sslContext = new SslContextBuilder(clientConfig.getSsl()).build();
            if (sslContext != null) {
                options.setSocketFactory(sslContext.getSocketFactory());
            }
        }

        factory.setConnectionOptions(options);
        return factory;
    }
}
