package com.fhs.cache.service;

import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.vo.VO;

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
    /**
     * 当前翻译字段相关信息
     * @param trans 当前的注解
     * @param value 当前字段的值
     * @param obj  当前字段的对象
     * @return     当前字段需要翻译后的结果值
     */
    String get(Trans trans, Serializable value, VO obj);
}
