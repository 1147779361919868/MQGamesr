package com.fhs.cache.service;

import com.fhs.common.constant.Constant;
import lombok.Data;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * redis 服务类
 *
 * @param <E>
 * @author wanglei
 */
@Data
public class RedisCacheService<E> {
    /**
     * redisTemplate
     */
    private RedisTemplate<String, E> redisTemplate;

    private RedisTemplate<String, String> strRedisTemplate;

    private Lock lock = new ReentrantLock();// 基于底层IO阻塞考虑


    public void put(String key, E obj) {
        ValueOperations<String, E> valueOper = redisTemplate.opsForValue();
        valueOper.set(key, obj);
    }


    public E get(String key) {
        ValueOperations<String, E> valueOper = redisTemplate.opsForValue();
        return valueOper.get(key);
    }


    public boolean contains(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }


    /**
     * 获取 RedisSerializer
     *
     * @return RedisSerializer
     */
    private RedisSerializer<String> getRedisSerializer() {
        return strRedisTemplate.getStringSerializer();
    }


    public Long removeFuzzy(final String key) {
        final String finalKey = key + "*";
        return redisTemplate.execute(new RedisCallback<Long>() {

            public Long doInRedis(RedisConnection connection)
                    throws DataAccessException {
                long result = 0;
                Set<byte[]> keys = connection.keys(finalKey.getBytes());
                for (byte[] keySet : keys) {
                    result += connection.del(keySet);
                }
                return result;
            }
        });
    }


    public void convertAndSend(String channel, String message) {
        redisTemplate.convertAndSend(channel, message);
    }


}
