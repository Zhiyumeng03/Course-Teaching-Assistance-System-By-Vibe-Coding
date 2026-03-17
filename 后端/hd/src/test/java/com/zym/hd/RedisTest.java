package com.zym.hd;

import com.zym.hd.course.entity.CourseMember;
import com.zym.hd.pojo.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@SpringBootTest
public class RedisTest {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    @Test
    public void test() {
        redisTemplate.opsForValue().set("CourseMember:1", new CourseMember(10L, 3L, "S20260001", "STUDENT under review", LocalDateTime.now()));
        System.out.println(redisTemplate.opsForValue().get("CourseMember:1"));
    }
}
