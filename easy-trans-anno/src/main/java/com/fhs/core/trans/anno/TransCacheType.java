package com.fhs.core.trans.anno;

/**
 * 缓存类型
 */
public enum TransCacheType {
    /**
     * redis缓存
     */
    REMOTE,
    /**
     * 本地缓存
     */
    LOCAL,
    /**
     * 2者都缓存
     */
    BOTH
}
