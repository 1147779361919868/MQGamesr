package com.fhs.trans.cache;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheManager;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.template.QuickConfig;
import com.fhs.core.trans.anno.TransCacheType;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 字典储存器
 */
public class DictCacheStorage {

    private static Map<TransCacheType, CacheType> cacheTypeSett = new HashMap<>();

    static {
        cacheTypeSett.put(TransCacheType.BOTH,CacheType.BOTH);
        cacheTypeSett.put(TransCacheType.REMOTE,CacheType.REMOTE);
        cacheTypeSett.put(TransCacheType.LOCAL,CacheType.LOCAL);
    }


    @Autowired
    private CacheManager cacheManager;

    private Cache<String, String> transCache;

    private Cache<String, String> unTransCache;


    /**
     * 初始化
     * @param transCacheType
     */
    public void init(TransCacheType transCacheType) {
        QuickConfig qc = QuickConfig.newBuilder("dictCache")
                .cacheType(cacheTypeSett.get(transCacheType)) // two level cache
                .syncLocal(true) // invalidate local cache in all jvm process after update
                .build();
        transCache = cacheManager.getOrCreateCache(qc);
        qc = QuickConfig.newBuilder("dictUntransCache")
                .cacheType(cacheTypeSett.get(transCacheType)) // two level cache
                .syncLocal(true) // invalidate local cache in all jvm process after update
                .build();
        unTransCache = cacheManager.getOrCreateCache(qc);
    }

    /**
     * 添加字典
     *
     * @param groupCode 字典分组编码
     * @param dictCode  字典编码
     * @param desc      描述
     */
    public void put(String groupCode, String dictCode, String desc) {
        transCache.put(groupCode + "_" + dictCode, desc);
        unTransCache.put(groupCode + "_" + desc,dictCode);
    }

    /**
     * 获取字典
     * @param groupCode 字典分组编码
     * @param dictCode  字典编码
     */
    public String get(String groupCode, String dictCode) {
        return transCache.get(groupCode + "_" + dictCode);
    }

    /**
     * 反向翻译
     * @param groupCode 字典分组编码
     * @param desc 字典描述
     * @return
     */
    public String unTrans(String groupCode, String desc){
        return unTransCache.get(groupCode + "_" + desc);
    }


}
