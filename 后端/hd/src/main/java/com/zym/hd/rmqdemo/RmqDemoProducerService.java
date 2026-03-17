package com.zym.hd.rmqdemo;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "demo.rocketmq", name = "enabled", havingValue = "true")
public class RmqDemoProducerService {

    private static final Logger log = LoggerFactory.getLogger(RmqDemoProducerService.class);
    private static final String DEFAULT_MESSAGE = "你好，我是生产者，这是一条业务启动示例消息。";

    private final RocketMQTemplate rocketMQTemplate;
    private final RmqDemoProperties rmqDemoProperties;

    public RmqDemoProducerService(RocketMQTemplate rocketMQTemplate, RmqDemoProperties rmqDemoProperties) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.rmqDemoProperties = rmqDemoProperties;
    }

    public void sendToConsumer(String message) {
        String finalMessage = (message == null || message.isBlank()) ? DEFAULT_MESSAGE : message;
        log.info("[生产者] 发送消息，topic={}，message={}", rmqDemoProperties.getRequestTopic(), finalMessage);
        rocketMQTemplate.convertAndSend(rmqDemoProperties.getRequestTopic(), finalMessage);
    }
}

