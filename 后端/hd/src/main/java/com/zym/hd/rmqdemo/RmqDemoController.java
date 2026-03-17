package com.zym.hd.rmqdemo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/demo/rmq")
@ConditionalOnProperty(prefix = "demo.rocketmq", name = "enabled", havingValue = "true")
public class RmqDemoController {

    private final RmqDemoProducerService rmqDemoProducerService;
    private final RmqDemoProperties rmqDemoProperties;

    public RmqDemoController(RmqDemoProducerService rmqDemoProducerService, RmqDemoProperties rmqDemoProperties) {
        this.rmqDemoProducerService = rmqDemoProducerService;
        this.rmqDemoProperties = rmqDemoProperties;
    }

    @GetMapping("/send")
    public Map<String, Object> send(@RequestParam(value = "message", required = false) String message) {
        rmqDemoProducerService.sendToConsumer(message);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("requestTopic", rmqDemoProperties.getRequestTopic());
        response.put("replyTopic", rmqDemoProperties.getReplyTopic());
        response.put("message", message == null || message.isBlank() ? "你好，我是生产者，这是一条业务启动示例消息。" : message);
        response.put("tip", "查看后端日志，可看到消费者处理和回执打印。");
        return response;
    }
}

