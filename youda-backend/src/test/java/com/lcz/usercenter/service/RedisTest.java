package com.lcz.usercenter.service;

import com.lcz.usercenter.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

/**
 * @author 1onetw
 * @version 1.0
 * @Description:
 */
@SpringBootTest
public class RedisTest {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Test
    public void test(){
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        // 增
        valueOperations.set("zhouString", "fish");
        valueOperations.set("zhouInt", 1);
        valueOperations.set("zhouDouble", 2.0);
        User user = new User();
        user.setId(1L);
        user.setUsername("zhou");
        valueOperations.set("zhouUser", user);

        // 查
        Object zhou = valueOperations.get("zhouString");
        Assertions.assertEquals("fish", (String) zhou);
        zhou = valueOperations.get("zhouInt");
        Assertions.assertEquals(1, (int) (Integer) zhou);
        zhou = valueOperations.get("zhouDouble");
        Assertions.assertEquals(2.0, (Double) zhou);
        System.out.println(valueOperations.get("zhouUser"));

        //删
//        redisTemplate.delete("zhouString");
    }
}
