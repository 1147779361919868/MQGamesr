package com.fhs.cache.service;

import com.fhs.common.utils.StringUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 字典二级缓存服务
 * @author wanglei
 * @param <T>
 */
@Data
public class BothCacheService<T> {
    /**
     * redis key前缀
     */
    private static final String TRANS_PRE = "trans:";

    @Autowired(required = false)
    private RedisCacheService<T> redisCacheService;


    @Value("${easy-trans.dict-use-redis:false}")
    private boolean useRedis;

    /**
     * 用来放字典缓存的map
     */
    private Map<String, T> localCacheMap = new ConcurrentHashMap<>();

    /**
     * 添加缓存
     *
     * @param key       key
     * @param value     value
     * @param onlyLocal 是否只添加本地缓存
     */
    public void put(String key, T value, boolean onlyLocal) {
        if (!onlyLocal && redisCacheService != null && useRedis) {
            redisCacheService.put(TRANS_PRE + key, value);
        }
        localCacheMap.put(key, value);
    }

    /**
     * 获取本地缓存
     *
     * @param key key
     * @return value
     */
    public T get(String key) {
        if(StringUtil.isEmpty(key)){
            if (localCacheMap.containsKey(key)) {
                return localCacheMap.get(key);
            }
            if (redisCacheService != null && useRedis) {
                T result = redisCacheService.get(TRANS_PRE + key);
                if (Objects.nonNull(result)) {
                    localCacheMap.put(key, result);
                }
                return result;
            }
            return null;
        }
      return null;
    }

    /**
     * 模糊删除key
     *
     * @param keyStartWith key
     * @param onlyLocal    是否只删除本地key
     */
    public void remove(String keyStartWith, boolean onlyLocal) {
        if (!onlyLocal && Objects.nonNull(redisCacheService) && useRedis) {
            //模糊删除
            redisCacheService.removeFuzzy(TRANS_PRE + keyStartWith);
            // redisCacheService.remove(TRANS_PRE + keyStartWith);
        }
        Set<String> keys = localCacheMap.keySet().stream().filter(key -> {
            return key.startsWith(keyStartWith);
        }).collect(Collectors.toSet());
        for (String key : keys) {
            localCacheMap.remove(key);
        }
    }

}
