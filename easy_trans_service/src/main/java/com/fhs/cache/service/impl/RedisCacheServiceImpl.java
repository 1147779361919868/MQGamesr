package com.fhs.cache.service.impl;

import com.fhs.cache.service.RedisCacheService;
import com.fhs.common.constant.Constant;
import lombok.Data;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * redis 缓存 服务类
 *
 * @author wanglei
 * @version [版本号, 2015年8月5日]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
@Data
public class RedisCacheServiceImpl<E> implements RedisCacheService<E> {
    /**
     * redisTemplate
     */

    private RedisTemplate<String, E> redisTemplate;


    private RedisTemplate<String, String> strRedisTemplate;

    private Lock lock = new ReentrantLock();// 基于底层IO阻塞考虑

    @Override
    public void put(String key, E obj) {
        ValueOperations<String, E> valueOper = redisTemplate.opsForValue();
        valueOper.set(key, obj);
    }

    @Override
    public E get(String key) {
        ValueOperations<String, E> valueOper = redisTemplate.opsForValue();
        return valueOper.get(key);
    }

    @Override
    public List<E> getList(String key) {
        ListOperations<String, E> valueOper = redisTemplate.opsForList();
        return valueOper.range(key, 0, -1);
    }

    @Override
    public Long remove(final String key) {
        if (!exists(key)) {
            return 0l;
        }
        return redisTemplate.execute(new RedisCallback<Long>() {
            @Override
            public Long doInRedis(RedisConnection connection)
                    throws DataAccessException {
                long result = 0;
                result = connection.del(key.getBytes());
                return result;
            }
        });
    }

    @Override
    public boolean exists(final String key) {
        return redisTemplate.execute(new RedisCallback<Boolean>() {
            public Boolean doInRedis(RedisConnection connection)
                    throws DataAccessException {
                return connection.exists(key.getBytes());
            }
        });
    }

    @Override
    public void addSet(String key, E[] objs) {
        ListOperations<String, E> list = redisTemplate.opsForList();
        for (E obj : objs) {
            list.leftPush(key, obj);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addSet(String key, Set<E> list) {
        this.addSet(key, (E[]) list.toArray());
    }

    @Override
    public void addSet(String key, E value) {
        redisTemplate.opsForSet().add(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addSet(String key, List<E> objSet) {
        this.addSet(key, (E[]) objSet.toArray());
    }

    @Override
    public boolean contains(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    @Override
    public void removeSetValue(String key, Object value) {
        redisTemplate.opsForSet().remove(key, value);
    }

    @Override
    public boolean addStr(final String key, final String value) {
        if (this.exists(key)) {
            this.updateStr(key, value);
        }
        boolean result = strRedisTemplate.execute(new RedisCallback<Boolean>() {
            public Boolean doInRedis(RedisConnection connection)
                    throws DataAccessException {
                RedisSerializer<String> serializer = getRedisSerializer();
                byte[] keys = serializer.serialize(key);
                byte[] values = serializer.serialize(value);
                return connection.setNX(keys, values);
            }
        });
        return result;
    }

    @Override
    public boolean updateStr(final String key, final String value) {
        if (!this.exists(key)) {
            return this.addStr(key, value);
        }
        return strRedisTemplate.execute(new RedisCallback<Boolean>() {
            public Boolean doInRedis(RedisConnection connection)
                    throws DataAccessException {
                RedisSerializer<String> serializer = getRedisSerializer();
                byte[] keys = serializer.serialize(key);
                byte[] values = serializer.serialize(value);
                connection.set(keys, values);
                return true;
            }
        });
    }

    @Override
    public String getStr(final String key) {
        String result = strRedisTemplate.execute(new RedisCallback<String>() {
            public String doInRedis(RedisConnection connection)
                    throws DataAccessException {
                RedisSerializer<String> serializer = getRedisSerializer();
                byte[] keys = serializer.serialize(key);
                byte[] values = connection.get(keys);
                if (values == null) {
                    return null;
                }
                String value = serializer.deserialize(values);
                return value;
            }
        });
        return result == null ? "" : result;
    }

    /**
     * 获取 RedisSerializer
     *
     * @return RedisSerializer
     */
    private RedisSerializer<String> getRedisSerializer() {
        return strRedisTemplate.getStringSerializer();
    }

    @Override
    public Long removeFuzzy(final String key) {
        return redisTemplate.execute(new RedisCallback<Long>() {
            @Override
            public Long doInRedis(RedisConnection connection)
                    throws DataAccessException {
                long result = 0;
                Set<byte[]> keys = connection.keys(key.getBytes());
                for (byte[] keySet : keys) {
                    result += connection.del(keySet);
                }
                return result;
            }
        });
    }

    @Override
    public Set<E> getSet(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    @Override
    public boolean expire(final String key, final int timeout) {
        return redisTemplate.execute(new RedisCallback<Long>() {
            @Override
            public Long doInRedis(RedisConnection connection)
                    throws DataAccessException {
                if (connection.expire(key.getBytes(), timeout)) {
                    return (long) Constant.SUCCESS_CODE;
                }
                ;
                return (long) Constant.DEFEAT_CODE;
            }
        }) == Constant.SUCCESS_CODE;


    }

    @Override
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 递增值
     *
     * @param key
     */
    public long incrAdd(String key) {
        return redisTemplate.boundValueOps(key).increment(1);
    }

    /**
     * 增加固定值
     *
     * @param key
     * @param value
     */
    public long incrAdd(String key, int value) {
        return redisTemplate.boundValueOps(key).increment(value);
    }

    /**
     * 递减值
     *
     * @param key
     */
    public long incrSub(String key) {
        return redisTemplate.boundValueOps(key).increment(-1);
    }

    @Override
    public void convertAndSend(String channel, String message) {
        redisTemplate.convertAndSend(channel, message);
    }

    @Override
    public Long getForListSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    @Override
    public void leftPush(String key, E value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    @Override
    public void rightPush(String key, E value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    @Override
    public E getBLPop(final String key) {
        return (E) redisTemplate.execute(new RedisCallback<E>() {
            public E doInRedis(RedisConnection connection)
                    throws DataAccessException {
                try {
                    lock.lockInterruptibly();

                    List<byte[]> results = connection.bLPop(0, key.getBytes());
                    if (CollectionUtils.isEmpty(results)) {
                        return null;
                    }
                    return (E) getRedisSerializer().deserialize(results.get(1));
                } catch (InterruptedException e) {
                } finally {
                    lock.unlock();
                }
                return null;
            }
        });
    }

    @Override
    public E getBRPop(final String key) {
        return (E) redisTemplate.execute(new RedisCallback<E>() {
            public E doInRedis(RedisConnection connection)
                    throws DataAccessException {
                try {
                    lock.lockInterruptibly();
                    List<byte[]> results = connection.bRPop(0, key.getBytes());
                    if (CollectionUtils.isEmpty(results)) {
                        return null;
                    }
                    RedisSerializer<?> redisSerializer = redisTemplate.getValueSerializer();
                    return (E) redisSerializer.deserialize(results.get(1));
                } catch (InterruptedException e) {
                } finally {
                    lock.unlock();
                }
                return null;
            }
        });
    }

}
