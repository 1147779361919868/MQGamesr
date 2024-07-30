package com.fhs.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 请看test包下的测试用例代码
 */
//@SpringBootApplication(exclude = { RedisAutoConfiguration.class })
@SpringBootApplication()
@EnableConfigurationProperties
public class TestApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(TestApplication.class, args);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
