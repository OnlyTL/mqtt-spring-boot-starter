package com.onlytl.mqtt.spring.boot.starter.annotation;


import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * <p>
 * MqttClient
 * </p >
 *
 * @author TL
 * @version 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface MqttClient {
    /**
     * MQTT客户端ID
     */
    String value() default "";

    /**
     * 客户端名称，用于配置中引用
     */
    String name();
}
