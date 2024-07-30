package com.fhs.trans.config;

import com.fhs.cache.service.RedisCacheService;
import com.fhs.cache.service.impl.RedisCacheServiceImpl;
import com.fhs.common.spring.SpringContextUtil;
import com.fhs.trans.listener.TransMessageListener;
import com.fhs.trans.service.impl.AutoTransService;
import com.fhs.trans.service.impl.DictionaryTransService;
import com.fhs.trans.service.impl.TransService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@Configuration
public class TransServiceConfig {

    /**
     * 是否启用redis
     */
    @Value("${easy-trans.is-enable-redis:false}")
    private boolean isEnableRedis;


    /**
     * service的包路径
     */
    @Value("${easy-trans.autotrans.package:com.*.*.service.impl}")
    private String packageNames;


    /**
     * 翻译服务主服务
     * @return
     */
    @Bean
    public TransService transService() {
        return new TransService();
    }

    /**
     * 自动翻译服务
     * @return
     */
    @Bean
    @DependsOn("springContextUtil")
    public AutoTransService autoTransService() {
        AutoTransService autoTransService = new AutoTransService();
        autoTransService.setPackageNames(packageNames);
        return autoTransService;
    }

    /**
     * 字典翻译服务
     * @return
     */
    @Bean
    public DictionaryTransService dictionaryTransService() {
        AutoTransService autoTransService = new AutoTransService();
        autoTransService.setPackageNames(packageNames);
        return new DictionaryTransService();
    }

    @Bean
    @ConditionalOnProperty(name = "easy-trans.is-enable-redis", havingValue = "true")
    public TransMessageListener transMessageListener(){
        return new TransMessageListener();
    }

    /**
     * redis消息监听器容器
     * 可以添加多个监听不同话题的redis监听器，只需要把消息监听器和相应的消息订阅处理器绑定，该消息监听器
     * 通过反射技术调用消息订阅处理器的相关方法进行一些业务处理
     * @param connectionFactory redis连接池
     * @param listenerAdapter 监听适配器
     * @return
     */
    @Bean
    @ConditionalOnProperty(name = "easy-trans.is-enable-redis", havingValue = "true")
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter){
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        //订阅多个频道
        container.addMessageListener(listenerAdapter,new PatternTopic("trans"));
        //序列化对象（特别注意：发布的时候需要设置序列化；订阅方也需要设置序列化）
        StringRedisSerializer seria = new StringRedisSerializer();
        container.setTopicSerializer(seria);
        return container;
    }

    //表示监听一个频道
    @Bean
    @ConditionalOnProperty(name = "easy-trans.is-enable-redis", havingValue = "true")
    MessageListenerAdapter listenerAdapter(TransMessageListener receiver){
        MessageListenerAdapter result = new MessageListenerAdapter(receiver,"handelMsg");
        JdkSerializationRedisSerializer valSerializer =new JdkSerializationRedisSerializer();
        result.setSerializer(valSerializer);
        return result;
    }

    @Bean
    @ConditionalOnProperty(name = "easy-trans.is-enable-redis", havingValue = "true")
    public RedisCacheService redisCacheService(RedisTemplate redisTemplate,AutoTransService autoTransService){
        RedisCacheServiceImpl redisCacheService = new RedisCacheServiceImpl();
        redisCacheService.setRedisTemplate(redisTemplate);
        redisCacheService.setStrRedisTemplate(redisTemplate);
        autoTransService.setRedisTransCache(redisCacheService);
        return redisCacheService;
    }




    @Bean("springContextUtil")
    public SpringContextUtil springContextUtil(){
        return new SpringContextUtil();
    }
}
