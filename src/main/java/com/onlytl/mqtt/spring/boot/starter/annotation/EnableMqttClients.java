package com.onlytl.mqtt.spring.boot.starter.annotation;


import com.onlytl.mqtt.spring.boot.starter.config.MqttAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * <p>
 * EnableMqttClients
 * </p >
 *
 * @author TL
 * @version 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MqttAutoConfiguration.class)
public @interface EnableMqttClients {
}
