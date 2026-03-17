package com.zym.hd.rmqdemo;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "demo.rocketmq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = "${demo.rocketmq.request-topic:course-demo-topic}",
        consumerGroup = "course-demo-consumer-group")
public class RmqDemoConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(RmqDemoConsumer.class);

    private final RocketMQTemplate rocketMQTemplate;
    private final RmqDemoProperties rmqDemoProperties;

    public RmqDemoConsumer(RocketMQTemplate rocketMQTemplate, RmqDemoProperties rmqDemoProperties) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.rmqDemoProperties = rmqDemoProperties;
    }

    @Override
    public void onMessage(String message) {
        log.info("[消费者] 收到消息，topic={}，message={}", rmqDemoProperties.getRequestTopic(), message);

        String reply = "消费者回执：我已处理 -> " + message;
        rocketMQTemplate.convertAndSend(rmqDemoProperties.getReplyTopic(), reply);
        log.info("[消费者] 发送回执，topic={}，message={}", rmqDemoProperties.getReplyTopic(), reply);
    }
}

