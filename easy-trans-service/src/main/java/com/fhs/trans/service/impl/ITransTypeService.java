package com.fhs.trans.service.impl;


import com.fhs.common.constant.TransConfig;
import com.fhs.common.utils.CheckUtils;
import com.fhs.common.utils.ConverterUtils;
import com.fhs.common.utils.StringUtil;
import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 翻译接口,将此接口实现类注册到transservice即可用
 *
 * @author wanglei
 */
public interface ITransTypeService {

    Logger Logger = LoggerFactory.getLogger(ITransTypeService.class);

    /**
     * 全局翻译map
     */
    Map<String, Cache<Object, Map<String, Object>>> GLOBAL_TRANS_CACHE = new HashMap<>();


    /**
     * 放缓存到全局中
     *
     * @param transResultMap 翻译结果
     * @param isAccess       true access多少秒过期，false 按照插入时间 true按照最后访问时间
     * @param cacheSeconds   缓存秒数
     * @param pkey           主键
     * @param namespace      类型
     * @param transType      翻译类型
     */
    default void put2GlobalCache(Map<String, Object> transResultMap, boolean isAccess, long cacheSeconds, int max, Object pkey, String namespace, String transType) {
        synchronized (GLOBAL_TRANS_CACHE) {
            if (GLOBAL_TRANS_CACHE.containsKey(transType + namespace)) {
                Cache<Object, Map<String, Object>> namespaceCache = GLOBAL_TRANS_CACHE.get(transType + namespace);
                namespaceCache.put(ConverterUtils.toString(pkey), transResultMap);
            } else {
                CacheBuilder builder = CacheBuilder.newBuilder();
                builder.maximumSize(max);
                if (isAccess) {
                    builder.expireAfterAccess(cacheSeconds, TimeUnit.SECONDS);
                } else {
                    builder.expireAfterWrite(cacheSeconds, TimeUnit.SECONDS);
                }
                Cache<Object, Map<String, Object>> namespaceCache = builder.build();
                namespaceCache.put(ConverterUtils.toString(pkey), transResultMap);
                GLOBAL_TRANS_CACHE.put(transType + namespace, namespaceCache);
            }
        }
    }

    /**
     * 清理掉缓存
     * @param pkey  主键
     * @param namespace 命名空间
     * @param transType 翻译类型
     */
    default void clearGlobalCache(Object pkey, String namespace, String transType){
        if (GLOBAL_TRANS_CACHE.containsKey(transType + namespace)) {
            Cache<Object, Map<String, Object>> namespaceCache = GLOBAL_TRANS_CACHE.get(transType + namespace);
            namespaceCache.invalidate(ConverterUtils.toString(pkey));
        }
    }

    /**
     * 从现有缓存获取一个缓存
     *
     * @param pkey      主键
     * @param namespace 类型
     * @param transType 翻译类型
     * @return
     */
    default Map<String, Object> getFromGlobalCache(Object pkey, String namespace, String transType) {
        synchronized (GLOBAL_TRANS_CACHE) {
            if (GLOBAL_TRANS_CACHE.containsKey(transType + namespace)) {
                Cache<Object, Map<String, Object>> namespaceCache = GLOBAL_TRANS_CACHE.get(transType + namespace);
                return namespaceCache.getIfPresent(ConverterUtils.toString(pkey));
            }
        }
        return null;
    }

    /**
     * 把全局有的缓存放到threadlocal里面去，并且把id在ids过滤掉
     * @param threadLocalCache
     * @param ids
     * @param namespace
     * @param transType
     * @return
     */
    default Set<Object> initLocalFromGlobalCache(ThreadLocal<Map<String, Map<String, Object>>> threadLocalCache, Set<Object> ids, String namespace, String transType) {
        Set<Object> resultIds = new HashSet<>();
        for (Object id : ids) {
            Map<String, Object> transResultMap = getFromGlobalCache(id, namespace, transType);
            if (transResultMap != null) {
                threadLocalCache.get().put(namespace + "_" + id,transResultMap);
            }else{
                resultIds.add(id);
            }
        }
        return resultIds;
    }


    /**
     * 翻译一个字段
     *
     * @param obj         需要翻译的对象
     * @param toTransList 需要翻译的字段
     */
    void transOne(VO obj, List<Field> toTransList);

    /**
     * 翻译多个 字段
     *
     * @param objList     需要翻译的对象集合
     * @param toTransList 需要翻译的字段集合
     */
    void transMore(List<? extends VO> objList, List<Field> toTransList);


    /**
     * 设置ref
     *
     * @param trans 注解对象
     * @param vo    等待被翻译的数据
     * @param val   翻译的值
     */
    default void setRef(Trans trans, VO vo, String val) {
        if (CheckUtils.isNotEmpty(trans.ref())) {
            ReflectUtils.setValue(vo, trans.ref(), val);
        }
        if (CheckUtils.isNotEmpty(trans.refs())) {
            Stream.of(trans.refs()).forEach(ref -> ReflectUtils.setValue(vo, ref, val));
        }
    }

    default void setRef(Trans trans, VO vo, Map<String, ?> valMap) {
        if (CheckUtils.isNotEmpty(trans.ref())) {
            setRef(trans.ref(), vo, valMap);
        }
        if (CheckUtils.isNotEmpty(trans.refs())) {
            for (int i = 0; i < trans.refs().length; i++) {
                setRef(trans.refs()[i], vo, valMap, i);
            }
        }
    }

    default void setRef(Trans trans, VO vo, Map<String, ?> valMap, VO target) {
        if (CheckUtils.isNotEmpty(trans.ref())) {
            boolean isSetRef = false;
            if (target != null) {
                Field field = ReflectUtils.getDeclaredField(vo.getClass(), trans.ref());
                if (field.getType() == target.getClass()) {
                    isSetRef = true;
                    ReflectUtils.setValue(vo, trans.ref(), target);
                }
            }
            //没有set才去set
            if (!isSetRef) {
                setRef(trans.ref(), vo, valMap);
            }
        }
        if (CheckUtils.isNotEmpty(trans.refs())) {
            for (int i = 0; i < trans.refs().length; i++) {
                setRef(trans.refs()[i], vo, valMap, i);
            }
        }
    }

    default void setRef(String ref, VO vo, Map<String, ?> valMap) {
        setRef(ref, vo, valMap, null);
    }


    default void setRef(String ref, VO vo, Map<String, ?> valMap, Integer index) {
        if (index == null) {
            if (valMap.size() > 0) {
                String key = valMap.keySet().iterator().next();
                ReflectUtils.setValue(vo, ref, valMap.get(key));
            }
        } else {
            ReflectUtils.setValue(vo, ref, valMap.get(new ArrayList<>(valMap.keySet()).get(index)));
        }
    }

    /**
     * 支持多库
     *
     * @param callable
     * @param dataSourceName
     * @return
     */
    default List<? extends VO> findByIds(Callable<List<? extends VO>> callable, String dataSourceName) {
        if (!TransConfig.MULTIPLE_DATA_SOURCES) {
            try {
                return callable.call();
            } catch (Exception e) {
                Logger.error("", e);
            }
            return null;
        }
        CompletableFuture<List<? extends VO>> cf = CompletableFuture.supplyAsync(() -> {
            try {
                if (!StringUtil.isEmpty(dataSourceName)) {
                    TransConfig.dataSourceSetter.setDataSource(dataSourceName);
                }
                return callable.call();
            } catch (Exception e) {
                Logger.error("", e);
            }
            return null;
        });
        try {
            return cf.get();
        } catch (InterruptedException e) {
            Logger.error("", e);
        } catch (ExecutionException e) {
            Logger.error("", e);
        }
        return null;
    }

    /**
     * 支持多库
     *
     * @param callable
     * @param dataSourceName
     * @return
     */
    default VO findById(Callable<VO> callable, String dataSourceName) {
        if (!TransConfig.MULTIPLE_DATA_SOURCES) {
            try {
                return callable.call();
            } catch (Exception e) {
                Logger.error("", e);
            }
            return null;
        }
        CompletableFuture<VO> cf = CompletableFuture.supplyAsync(() -> {
            try {
                if (!StringUtil.isEmpty(dataSourceName)) {
                    TransConfig.dataSourceSetter.setDataSource(dataSourceName);
                }
                return callable.call();
            } catch (Exception e) {
                Logger.error("", e);
            }
            return null;
        });
        try {
            return cf.get();
        } catch (InterruptedException e) {
            Logger.error("", e);
        } catch (ExecutionException e) {
            Logger.error("", e);
        }
        return null;
    }
}
