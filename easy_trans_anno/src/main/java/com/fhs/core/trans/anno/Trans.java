package com.fhs.core.trans.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 翻译
 * @author wanglei
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface Trans {

    /**
     * 获取翻译类型，比如 wordbook 是字典
     * @return 类型
     */
    String type();

    /**
     * 字段 比如  要翻译男女 上面的type写wordbook 此key写sex即可
     * @return
     */
    String key() default "";
}
