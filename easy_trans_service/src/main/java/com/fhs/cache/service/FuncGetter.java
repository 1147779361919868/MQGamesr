package com.fhs.cache.service;

import com.fhs.core.trans.anno.Trans;

import java.io.Serializable;

/**
 * 自定义获取值的条件下需实现此接口
 *
 * @author liangbaikai
 * @version [版本号, 2021年10月22日]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
@FunctionalInterface
public interface FuncGetter {

    String get(Trans trans, Serializable value);
}
