package com.onlytl.mqtt.spring.boot.starter.config;


import com.onlytl.mqtt.spring.boot.starter.annotation.MqttClient;
import com.onlytl.mqtt.spring.boot.starter.annotation.MqttSubscribe;
import com.onlytl.mqtt.spring.boot.starter.factory.MqttClientFactory;
import com.onlytl.mqtt.spring.boot.starter.handler.MqttMessageHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * MqttClientConfiguration
 * </p >
 *
 * @author TL
 * @version 1.0.0
 */
@Slf4j
public class MqttClientConfiguration implements BeanPostProcessor,
        ApplicationListener<ContextRefreshedEvent>,
        DisposableBean,
        BeanFactoryAware {

    private final MqttProperties mqttProperties;
    private final MqttClientFactory mqttClientFactory;
    private final MqttMessageHandler defaultMqttMessageHandler;

    @Autowired
    private ThreadPoolTaskScheduler mqttTaskScheduler;

    // 存储客户端工厂
    private final Map<String, MqttPahoClientFactory> clientFactories = new ConcurrentHashMap<>();

    // 存储MQTT出站处理器
    @Getter
    private final Map<String, MqttPahoMessageHandler> outboundHandlers = new ConcurrentHashMap<>();

    // 存储MQTT入站适配器
    private final Map<String, MqttPahoMessageDrivenChannelAdapter> inboundAdapters = new ConcurrentHashMap<>();

    // 存储消息通道
    private final Map<String, DirectChannel> channels = new ConcurrentHashMap<>();

    // 存储订阅信息
    private final Map<String, List<SubscriptionInfo>> subscriptions = new ConcurrentHashMap<>();

    private boolean initialized = false;

    @Autowired
    public MqttClientConfiguration(MqttProperties mqttProperties,
                                   MqttClientFactory mqttClientFactory,
                                   MqttMessageHandler defaultMqttMessageHandler) {
        this.mqttProperties = mqttProperties;
        this.mqttClientFactory = mqttClientFactory;
        this.defaultMqttMessageHandler = defaultMqttMessageHandler;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.isAopProxy(bean) ?
                AopProxyUtils.ultimateTargetClass(bean) : bean.getClass();

        // 处理MqttClient注解
        MqttClient mqttClientAnnotation = AnnotationUtils.findAnnotation(targetClass, MqttClient.class);
        if (mqttClientAnnotation != null) {
            log.info("Found MQTT client: {}", mqttClientAnnotation.name());
        }

        // 查找带有MqttSubscribe注解的方法
        ReflectionUtils.doWithMethods(targetClass, method -> {
            MqttSubscribe mqttSubscribe = AnnotationUtils.findAnnotation(method, MqttSubscribe.class);
            if (mqttSubscribe != null) {
                registerSubscription(bean, method, mqttSubscribe);
            }
        });

        return bean;
    }

    private void registerSubscription(Object bean, Method method, MqttSubscribe mqttSubscribe) {
        String topic = mqttSubscribe.topic().isEmpty() ? mqttSubscribe.value() : mqttSubscribe.topic();
        Assert.hasText(topic, "Topic must be specified in @MqttSubscribe annotation");

        String clientName = mqttSubscribe.client();
        int qos = mqttSubscribe.qos();

        log.info("Registering MQTT subscription: topic={}, qos={}, client={}, method={}.{}",
                topic, qos, clientName, bean.getClass().getSimpleName(), method.getName());

        // 将订阅信息存储起来，等待context刷新后统一处理
        SubscriptionInfo subscriptionInfo = new SubscriptionInfo(bean, method, topic, qos);
        subscriptions.computeIfAbsent(clientName, k -> new ArrayList<>()).add(subscriptionInfo);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (initialized || !mqttProperties.isEnabled()) {
            return;
        }

        try {
            // 初始化所有MQTT客户端
            initializeMqttClients();

            // 处理所有订阅
            processSubscriptions();

            initialized = true;
            log.info("MQTT clients initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize MQTT clients", e);
            throw new RuntimeException("Failed to initialize MQTT clients", e);
        }
    }

    private void initializeMqttClients() throws Exception {
        // 初始化默认客户端
        initializeMqttClient("default", mqttProperties.getDefaultClient());

        // 初始化其他客户端
        for (Map.Entry<String, MqttProperties.ClientConfig> entry : mqttProperties.getClients().entrySet()) {
            initializeMqttClient(entry.getKey(), entry.getValue());
        }
    }

    private void initializeMqttClient(String clientName, MqttProperties.ClientConfig config) throws Exception {
        // 创建MQTT客户端工厂
        MqttPahoClientFactory clientFactory = mqttClientFactory.createClientFactory(config);
        clientFactories.put(clientName, clientFactory);

        // 创建入站通道
        DirectChannel inboundChannel = new DirectChannel();
        channels.put(clientName + "-inbound", inboundChannel);

        // 创建出站通道
        DirectChannel outboundChannel = new DirectChannel();
        channels.put(clientName + "-outbound", outboundChannel);

        // 创建出站处理器
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(
                config.getClientId() + "-outbound", clientFactory);
        messageHandler.setAsync(true);
        if (config.getDefaultTopic() != null) {
            messageHandler.setDefaultTopic(config.getDefaultTopic());
        }
        messageHandler.setDefaultQos(config.getDefaultQos());
        outboundHandlers.put(clientName, messageHandler);

        log.debug("Initialized MQTT client: {}", clientName);
    }

    private void processSubscriptions() {
        // 为每个客户端创建订阅适配器
        for (Map.Entry<String, List<SubscriptionInfo>> entry : subscriptions.entrySet()) {
            String clientName = entry.getKey();
            List<SubscriptionInfo> clientSubscriptions = entry.getValue();

            if (clientSubscriptions.isEmpty()) {
                continue;
            }

            // 获取客户端配置
            MqttProperties.ClientConfig config = clientName.equals("default") ?
                    mqttProperties.getDefaultClient() : mqttProperties.getClients().get(clientName);

            if (config == null) {
                log.warn("No configuration found for MQTT client: {}, skipping subscriptions", clientName);
                continue;
            }

            // 获取客户端工厂
            MqttPahoClientFactory clientFactory = clientFactories.get(clientName);
            if (clientFactory == null) {
                log.warn("No factory found for MQTT client: {}, skipping subscriptions", clientName);
                continue;
            }

            // 获取入站通道
            DirectChannel inboundChannel = channels.get(clientName + "-inbound");

            // 创建入站适配器
            String[] topics = clientSubscriptions.stream()
                    .map(SubscriptionInfo::getTopic)
                    .distinct()
                    .toArray(String[]::new);

            int[] qos = clientSubscriptions.stream()
                    .map(SubscriptionInfo::getQos)
                    .mapToInt(Integer::intValue)
                    .toArray();

            MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                    config.getClientId() + "-inbound", clientFactory, topics);
            adapter.setQos(qos);
            adapter.setConverter(new DefaultPahoMessageConverter());
            adapter.setOutputChannel(inboundChannel);
            adapter.setCompletionTimeout(5000);
            adapter.setTaskScheduler(mqttTaskScheduler);

            // 启动适配器
            adapter.start();
            inboundAdapters.put(clientName, adapter);

            // 添加消息处理器
            inboundChannel.subscribe(message -> {
                String topic = (String) message.getHeaders().get("mqtt_receivedTopic");

                // 调用默认处理器
                defaultMqttMessageHandler.handleMessage(message, topic, clientName);

                // 调用特定的订阅方法
                clientSubscriptions.stream()
                        .filter(subscription -> {
                            assert topic != null;
                            return topicMatches(subscription.getTopic(), topic);
                        })
                        .forEach(subscription -> {
                            try {
                                ReflectionUtils.makeAccessible(subscription.getMethod());
                                if (subscription.getMethod().getParameterCount() == 1) {
                                    subscription.getMethod().invoke(subscription.getBean(), message.getPayload());
                                } else if (subscription.getMethod().getParameterCount() == 2) {
                                    subscription.getMethod().invoke(subscription.getBean(),
                                            message.getPayload(), topic);
                                } else if (subscription.getMethod().getParameterCount() == 3) {
                                    subscription.getMethod().invoke(subscription.getBean(),
                                            message.getPayload(), topic, clientName);
                                } else {
                                    subscription.getMethod().invoke(subscription.getBean());
                                }
                            } catch (Exception e) {
                                log.error("Error invoking subscription method: {}",
                                        subscription.getMethod().getName(), e);
                            }
                        });
            });

            log.info("Started MQTT subscription adapter for client: {} with topics: {}",
                    clientName, String.join(", ", topics));
        }
    }

    private boolean topicMatches(String subscription, String actualTopic) {
        // 将主题分割为段
        String[] subParts = subscription.split("/");
        String[] topicParts = actualTopic.split("/");

        // 如果订阅主题以 # 结尾，并且前面的所有部分都匹配，则匹配
        if (subParts.length > 0 && subParts[subParts.length - 1].equals("#")) {
            if (topicParts.length < subParts.length - 1) {
                return false;
            }

            for (int i = 0; i < subParts.length - 1; i++) {
                if (!subParts[i].equals("+") && !subParts[i].equals(topicParts[i])) {
                    return false;
                }
            }
            return true;
        }

        // 如果段数不同且不是 # 结尾，则不匹配
        if (subParts.length != topicParts.length) {
            return false;
        }

        // 检查每个段是否匹配
        for (int i = 0; i < subParts.length; i++) {
            if (!subParts[i].equals("+") && !subParts[i].equals(topicParts[i])) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void destroy() throws Exception {
        // 关闭所有入站适配器
        for (MqttPahoMessageDrivenChannelAdapter adapter : inboundAdapters.values()) {
            try {
                adapter.stop();
            } catch (Exception e) {
                log.warn("Error stopping MQTT adapter", e);
            }
        }

        log.info("MQTT clients destroyed");
    }

    public Map<String, MqttProperties.ClientConfig> getClientConfigs() {
        Map<String, MqttProperties.ClientConfig> configs = new HashMap<>();
        configs.put("default", mqttProperties.getDefaultClient());
        configs.putAll(mqttProperties.getClients());
        return configs;
    }

    // 订阅信息内部类
    @Getter
    private static class SubscriptionInfo {
        private final Object bean;
        private final Method method;
        private final String topic;
        private final int qos;

        public SubscriptionInfo(Object bean, Method method, String topic, int qos) {
            this.bean = bean;
            this.method = method;
            this.topic = topic;
            this.qos = qos;
        }

    }
}