package com.fhs.core.trans.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fhs.core.trans.util.ReflectUtils;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 支持vo 即可进行翻译
 *
 * @author wanglei
 * @date 2020-05-19 11:51:08
 */
public interface VO {

    /**
     * 子类id字段缓存
     */
    @TableField(exist = false)
    @JsonIgnore
    Map<Class<?>, Field> ID_FIELD_CACHE_MAP = new HashMap<>();

    Set<Class> ID_ANNO = new HashSet<>();


    ThreadLocal<Map<String, Map<String, String>>> TRANS_MAP_CACHE = new ThreadLocal<>();

    default void clearTransCache() {
        //缓存60秒过期，避免内存泄露
        Caffeine builder =  Caffeine.newBuilder();
        builder.expireAfterWrite(60, TimeUnit.SECONDS);
        TRANS_MAP_CACHE.set(builder.build().asMap());
    }

    /**
     * 获取翻译map
     *
     * @return 翻译map
     */
    default Map<String, String> getTransMap() {
        if (TRANS_MAP_CACHE.get() == null) {
            clearTransCache();
        }
        Map<String, Map<String, String>> cache = TRANS_MAP_CACHE.get();
        String cacheKey = this.getClass().getName() + "_" + this.getPkey();
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        Map<String, String> result = new LinkedHashMap<>();
        cache.put(cacheKey, result);
        return result;
    }

    /**
     * 获取主键
     *
     * @return 主键
     */
    @JsonIgnore
    @JSONField(serialize = false)
    default Object getPkey() {
        Field idField = ReflectUtils.getIdField(this.getClass(),true);
        try {
            return idField.get(this);
        } catch (IllegalAccessException e) {
            return null;
        }
    }





}
