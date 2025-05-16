package com.fhs.trans.config;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fhs.cache.service.RedisCacheService;
import com.fhs.cache.service.impl.RedisCacheServiceImpl;
import com.fhs.common.spring.SpringContextUtil;
import com.fhs.trans.advice.EasyTransResponseBodyAdvice;
import com.fhs.trans.aop.TransMethodResultAop;
import com.fhs.trans.controller.TransProxyController;
import com.fhs.trans.json.TransFastJsonHttpMessageConverter;
import com.fhs.trans.json.TransMappingJackson2HttpMessageConverter;
import com.fhs.trans.listener.TransMessageListener;
import com.fhs.trans.service.impl.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@ServletComponentScan({"com.fhs.trans.filter"})
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
     *
     * @return
     */
    @Bean
    public TransService transService() {
        return new TransService();
    }

    /**
     * 自动翻译服务
     *
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
     *
     * @return
     */
    @Bean
    public DictionaryTransService dictionaryTransService() {
        AutoTransService autoTransService = new AutoTransService();
        autoTransService.setPackageNames(packageNames);
        return new DictionaryTransService();
    }

    /**
     * 简单翻译
     *
     * @return
     */
    @Bean
    @ConditionalOnBean(SimpleTransService.SimpleTransDiver.class)
    public SimpleTransService simpleTransService(SimpleTransService.SimpleTransDiver dirver) {
        SimpleTransService result = new SimpleTransService();
        result.regsiterTransDiver(dirver);
        return result;
    }

    @Bean
    @LoadBalanced
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    /**
     * 远程翻译
     *
     * @return
     */
    @Bean
    @ConditionalOnBean(SimpleTransService.SimpleTransDiver.class)
    public RpcTransService rpcTransService(SimpleTransService.SimpleTransDiver dirver, RestTemplate restTemplate) {
        RpcTransService result = new RpcTransService();
        result.regsiterTransDiver(dirver);
        result.setRestTemplate(restTemplate);
        return result;
    }

    /**
     * 远程翻译调用代理
     *
     * @return
     */
    @Bean
    @ConditionalOnBean(SimpleTransService.SimpleTransDiver.class)
    public TransProxyController transProxyController(SimpleTransService.SimpleTransDiver dirver) {
        TransProxyController result = new TransProxyController();
        result.setSimpleTransDiver(dirver);
        return result;
    }

    /**
     * fastjson 消息转换器 使用fastjson进行平铺
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(FastJsonHttpMessageConverter.class)
    @ConditionalOnProperty(name = "easy-trans.tile", havingValue = "fastjson")
    public TransFastJsonHttpMessageConverter transFastJsonHttpMessageConverter() {
        TransFastJsonHttpMessageConverter result = new TransFastJsonHttpMessageConverter();
        return result;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "easy-trans.tile", havingValue = "jackson")
    public MappingJackson2HttpMessageConverter getMappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new TransMappingJackson2HttpMessageConverter();
        //设置日期格式
        ObjectMapper objectMapper = new ObjectMapper();
        mappingJackson2HttpMessageConverter.setObjectMapper(objectMapper);
        //设置中文编码格式
        List<MediaType> list = new ArrayList<MediaType>();
        list.add(MediaType. APPLICATION_JSON);
        list.add(MediaType. APPLICATION_JSON_UTF8);
        mappingJackson2HttpMessageConverter.setSupportedMediaTypes(list);
        return mappingJackson2HttpMessageConverter;
    }


    /**
     * 自动翻译方法结果aop
     *
     * @return
     */
    @Bean
    public TransMethodResultAop transMethodResultAop() {
        return new TransMethodResultAop();
    }

    @Bean
    @ConditionalOnProperty(name = "easy-trans.is-enable-global", havingValue = "true")
    public EasyTransResponseBodyAdvice EasyTransResponseBodyAdvice() {
        return new EasyTransResponseBodyAdvice();
    }


    @Bean
    @ConditionalOnProperty(name = "easy-trans.is-enable-redis", havingValue = "true")
    public TransMessageListener transMessageListener() {
        return new TransMessageListener();
    }

    /**
     * redis消息监听器容器
     * 可以添加多个监听不同话题的redis监听器，只需要把消息监听器和相应的消息订阅处理器绑定，该消息监听器
     * 通过反射技术调用消息订阅处理器的相关方法进行一些业务处理
     *
     * @param connectionFactory redis连接池
     * @param listenerAdapter   监听适配器
     * @return
     */
    @Bean
    @ConditionalOnProperty(name = "easy-trans.is-enable-redis", havingValue = "true")
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        //订阅多个频道
        container.addMessageListener(listenerAdapter, new PatternTopic("trans"));
        //序列化对象（特别注意：发布的时候需要设置序列化；订阅方也需要设置序列化）
        StringRedisSerializer seria = new StringRedisSerializer();
        container.setTopicSerializer(seria);
        return container;
    }

    //表示监听一个频道
    @Bean
    @ConditionalOnProperty(name = "easy-trans.is-enable-redis", havingValue = "true")
    MessageListenerAdapter listenerAdapter(TransMessageListener receiver) {
        MessageListenerAdapter result = new MessageListenerAdapter(receiver, "handelMsg");
        JdkSerializationRedisSerializer valSerializer = new JdkSerializationRedisSerializer();
        result.setSerializer(valSerializer);
        return result;
    }

    @Bean
    @ConditionalOnProperty(name = "easy-trans.is-enable-redis", havingValue = "true")
    public RedisCacheService redisCacheService(RedisTemplate redisTemplate, AutoTransService autoTransService) {
        RedisCacheServiceImpl redisCacheService = new RedisCacheServiceImpl();
        redisCacheService.setRedisTemplate(redisTemplate);
        redisCacheService.setStrRedisTemplate(redisTemplate);
        autoTransService.setRedisTransCache(redisCacheService);
        return redisCacheService;
    }


    @Bean("springContextUtil")
    public SpringContextUtil springContextUtil() {
        return new SpringContextUtil();
    }

}
