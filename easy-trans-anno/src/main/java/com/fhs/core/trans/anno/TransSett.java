package com.fhs.core.trans.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 翻译方法的返回结果
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface TransSett {
    /**
     * 翻译哪些字段
     * @return
     */
    String[] include() default {};

    /**
     * 排除翻译哪些字段
     * @return
     */
    String[] exclude() default {};
}
