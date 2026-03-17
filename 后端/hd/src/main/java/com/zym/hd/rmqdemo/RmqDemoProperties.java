package com.zym.hd.rmqdemo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "demo.rocketmq")
public class RmqDemoProperties {

    private boolean enabled = false;
    private String requestTopic = "course-demo-topic";
    private String replyTopic = "course-demo-reply-topic";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRequestTopic() {
        return requestTopic;
    }

    public void setRequestTopic(String requestTopic) {
        this.requestTopic = requestTopic;
    }

    public String getReplyTopic() {
        return replyTopic;
    }

    public void setReplyTopic(String replyTopic) {
        this.replyTopic = replyTopic;
    }
}

