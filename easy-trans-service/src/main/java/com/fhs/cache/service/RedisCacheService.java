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
 * redis 缓存 服务类
 *
 * @author wanglei
 * @version [版本号, 2015年8月5日]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
@Data
public class RedisCacheService<E>  {
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

    
    public List<E> getList(String key) {
        ListOperations<String, E> valueOper = redisTemplate.opsForList();
        return valueOper.range(key, 0, -1);
    }

    
    public Long remove(final String key) {
        if (!exists(key)) {
            return 0l;
        }
        return redisTemplate.execute(new RedisCallback<Long>() {
            
            public Long doInRedis(RedisConnection connection)
                    throws DataAccessException {
                long result = 0;
                result = connection.del(key.getBytes());
                return result;
            }
        });
    }

    
    public Long removeStr(final String key) {
        if (!existsStr(key)) {
            return 0l;
        }
        return strRedisTemplate.execute(new RedisCallback<Long>() {
            
            public Long doInRedis(RedisConnection connection)
                    throws DataAccessException {
                long result = 0;
                result = connection.del(key.getBytes());
                return result;
            }
        });
    }

    
    public boolean exists(final String key) {
        return redisTemplate.execute(new RedisCallback<Boolean>() {
            public Boolean doInRedis(RedisConnection connection)
                    throws DataAccessException {
                return connection.exists(key.getBytes());
            }
        });
    }

    
    public boolean existsStr(String key) {
        return strRedisTemplate.execute(new RedisCallback<Boolean>() {
            public Boolean doInRedis(RedisConnection connection)
                    throws DataAccessException {
                return connection.exists(key.getBytes());
            }
        });
    }

    
    public void addSet(String key, E[] objs) {
        SetOperations<String, E> set = redisTemplate.opsForSet();
        for (E obj : objs) {
            set.add(key, obj);
        }
    }

    @SuppressWarnings("unchecked")
    
    public void addSet(String key, Set<E> list) {
        this.addSet(key, (E[]) list.toArray());
    }

    
    public void addSet(String key, E value) {
        redisTemplate.opsForSet().add(key, value);
    }

    @SuppressWarnings("unchecked")
    
    public void addList(String key, List<E> objList) {
        ListOperations<String, E> list = redisTemplate.opsForList();
        for (E obj : objList) {
            list.leftPush(key, obj);
        }
    }

    
    public boolean contains(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    
    public void removeSetValue(String key, Object value) {
        redisTemplate.opsForSet().remove(key, value);
    }

    
    public boolean addStr(final String key, final String value) {
        if (this.existsStr(key)) {
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

    
    public Long removeFuzzy(final String key) {
        return redisTemplate.execute(new RedisCallback<Long>() {
            
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

    
    public Set<E> getSet(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    
    public boolean expire(final String key, final int timeout) {
        return redisTemplate.execute(new RedisCallback<Long>() {
            
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

    
    public boolean expireStr(final String key, final int timeout) {
        return strRedisTemplate.execute(new RedisCallback<Long>() {
            
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

    
    public void convertAndSend(String channel, String message) {
        redisTemplate.convertAndSend(channel, message);
    }

    
    public Long getForListSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    
    public void leftPush(String key, E value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    
    public void rightPush(String key, E value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    
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

    
    public Set<String> getFuzzy(String matchKey) {
        Set<String> keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keysTmp = new HashSet<>();
            Cursor<byte[]> cursor = connection.scan(new ScanOptions.ScanOptionsBuilder().match("*" + matchKey + "*").count(1000).build());
            while (cursor.hasNext()) {
                keysTmp.add(new String(cursor.next()));
            }
            return keysTmp;
        });
        return keys;
    }

}
