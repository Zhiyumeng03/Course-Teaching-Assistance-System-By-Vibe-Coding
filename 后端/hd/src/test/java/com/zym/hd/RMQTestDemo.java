package com.zym.hd;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

/**
 * RocketMQ + Spring 入门演示：
 * 1) 生产者向 REQUEST_TOPIC 发送消息
 * 2) 消费者订阅 REQUEST_TOPIC 并向 REPLY_TOPIC 发送回执
 * 3) 生产者侧监听器订阅 REPLY_TOPIC 并打印回执
 */
@SpringBootTest(classes = RMQTestDemo.TestApp.class)
@ActiveProfiles("rmq-demo")
public class RMQTestDemo {

    static final String REQUEST_TOPIC = "course-demo-topic";
    static final String REPLY_TOPIC = "course-demo-reply-topic";

    @Autowired
    private DemoProducer demoProducer;

    @Test
    void sendAndReceive() throws InterruptedException {
        demoProducer.sendToConsumer("你好，我是生产者，欢迎学习 RocketMQ。");
        // 消费是异步的，稍等片刻，便于在日志中看到完整消息链路。
        TimeUnit.SECONDS.sleep(3);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = RMQTestDemo.class)
    static class TestApp {

    }

    @Service
    static class DemoProducer {
        private static final Logger log = LoggerFactory.getLogger(DemoProducer.class);

        @Autowired
        private RocketMQTemplate rocketMQTemplate;

        public void sendToConsumer(String message) {
            log.info("[生产者] 发送消息，topic={}，message={}", REQUEST_TOPIC, message);
            rocketMQTemplate.convertAndSend(REQUEST_TOPIC, message);
        }
    }

    @Component
    @RocketMQMessageListener(topic = REQUEST_TOPIC, consumerGroup = "course-demo-consumer-group")
    static class DemoConsumer implements RocketMQListener<String> {
        private static final Logger log = LoggerFactory.getLogger(DemoConsumer.class);

        @Autowired
        private RocketMQTemplate rocketMQTemplate;

        @Override
        public void onMessage(String message) {
            log.info("[消费者] 收到消息，topic={}，message={}", REQUEST_TOPIC, message);

            String reply = "消费者回执：我已处理 -> " + message;
            rocketMQTemplate.convertAndSend(REPLY_TOPIC, reply);
            log.info("[消费者] 发送回执，topic={}，message={}", REPLY_TOPIC, reply);
        }
    }

    @Component
    @RocketMQMessageListener(topic = REPLY_TOPIC, consumerGroup = "course-demo-producer-reply-group")
    static class ProducerReplyPrinter implements RocketMQListener<String> {
        private static final Logger log = LoggerFactory.getLogger(ProducerReplyPrinter.class);

        @Override
        public void onMessage(String message) {
            log.info("[生产者侧] 收到消费者回执，topic={}，message={}", REPLY_TOPIC, message);
        }
    }
}
