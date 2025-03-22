package com.fhs.core.trans.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 获取需要翻译的类型
 *
 * @author wanglei
 * @date 2020-05-19 10:14:26
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface TransTypes {
    /**
     * 类型
     *
     * @return
     */
    String[] types();
}
