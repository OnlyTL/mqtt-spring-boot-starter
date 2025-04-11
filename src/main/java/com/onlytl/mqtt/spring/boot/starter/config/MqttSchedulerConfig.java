package com.onlytl.mqtt.spring.boot.starter.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * <p>
 * MqttSchedulerConfig
 * </p >
 *
 * @author TL
 * @version 1.0.0
 */
@Configuration
public class MqttSchedulerConfig {

    @Value("${mqtt.thread-pool.size:10}")
    private int size;

    /**
     * MQTT适配器需要一个TaskScheduler来管理连接和心跳
     */
    @Bean
    public ThreadPoolTaskScheduler mqttTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(size);
        scheduler.setThreadNamePrefix("mqtt-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}