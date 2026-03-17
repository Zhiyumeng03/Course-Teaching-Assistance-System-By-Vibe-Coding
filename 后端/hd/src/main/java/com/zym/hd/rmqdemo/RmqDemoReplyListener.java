package com.zym.hd.rmqdemo;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "demo.rocketmq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = "${demo.rocketmq.reply-topic:course-demo-reply-topic}",
        consumerGroup = "course-demo-producer-reply-group")
public class RmqDemoReplyListener implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(RmqDemoReplyListener.class);
    private final RmqDemoProperties rmqDemoProperties;

    public RmqDemoReplyListener(RmqDemoProperties rmqDemoProperties) {
        this.rmqDemoProperties = rmqDemoProperties;
    }

    @Override
    public void onMessage(String message) {
        log.info("[生产者侧] 收到消费者回执，topic={}，message={}", rmqDemoProperties.getReplyTopic(), message);
    }
}
