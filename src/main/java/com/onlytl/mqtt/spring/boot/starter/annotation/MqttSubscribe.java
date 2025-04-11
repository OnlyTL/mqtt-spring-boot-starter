package com.onlytl.mqtt.spring.boot.starter.annotation;


import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * <p>
 * MqttSubscribe
 * </p >
 *
 * @author TL
 * @version 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MqttSubscribe {

    /**
     * 订阅的主题
     */
    @AliasFor("topic")
    String value() default "";

    /**
     * 订阅的主题
     */
    @AliasFor("value")
    String topic() default "";

    /**
     * QoS质量等级：0, 1, 2
     */
    int qos() default 1;

    /**
     * 客户端名称，用于指定哪个MQTT客户端处理该订阅
     */
    String client() default "default";
}
