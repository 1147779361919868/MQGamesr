package com.fhs.cache.service;

import com.fhs.core.trans.anno.Trans;

import java.io.Serializable;

/**
 * @description:
 * @author：liangbaikai
 * @date: 2021-10-21
 * @Copyright：
 */
@FunctionalInterface
public interface FuncGetter {

    String get(Trans trans, Serializable value);
}
