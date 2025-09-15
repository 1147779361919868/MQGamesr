package com.fhs.core.trans.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fhs.core.trans.util.ReflectUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

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

    ThreadLocal<Map<String, Map<String, String>>> TRANS_MAP_CACHE = new ThreadLocal<>();

    default void clearTransCache() {
        //缓存60秒过期，避免内存泄露
        Cache<String, Map<String, String>> cache = CacheBuilder.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS).build();
        TRANS_MAP_CACHE.set(cache.asMap());
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
        String cacheKey = this.getClass().getName() + "_" + this.hashCode();
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
        Field idField = getIdField(true);
        try {
            return idField.get(this);
        } catch (IllegalAccessException e) {
            return null;
        }
    }


    /**
     * 获取子类id字段
     *
     * @return 子类id字段
     */
    @JsonIgnore
    @JSONField(serialize = false)
    default Field getIdField(boolean isThrowError) {
        if (ID_FIELD_CACHE_MAP.containsKey(this.getClass())) {
            return ID_FIELD_CACHE_MAP.get(this.getClass());
        }
        List<Field> fieldList = ReflectUtils.getAnnotationField(this.getClass(), javax.persistence.Id.class);
        if (fieldList.size() == 0) {
            fieldList = ReflectUtils.getAnnotationField(this.getClass(), TableId.class);
            if (fieldList.size() == 0) {
                if (isThrowError) {
                    throw new RuntimeException("找不到" + this.getClass() + "的id注解");
                }
            }
            fieldList.get(0).setAccessible(true);
            return fieldList.get(0);
        }
        fieldList.get(0).setAccessible(true);
        ID_FIELD_CACHE_MAP.put(this.getClass(), fieldList.get(0));
        return fieldList.get(0);
    }


}
