package com.fhs.core.trans.anno;

import java.lang.annotation.*;

/**
 * 翻译默认配置
 * 设置默认字段
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface TransDefaultSett {
    /**
     * 如果simple trans标记了使用了此注解标记的pojo，那么在 @Trans注解没有配置字段的时候，此处可以适配字段
     * @return
     */
    String[] defaultFields() default {};

    /**
     * 默认别名 比如有个name字段，其他的表也有name字段你这个类是Student类那么别名可以设置为student 翻译的时候取studentName避免和其他的类冲突
     * @return
     */
    String defaultAlias() default "";

    /**
     *  唯一键字段
     * 部分的时候表里的code，身份证号码，手机号等也是唯一键
     * @return
     */
    String uniqueField() default "";

    /**
     * 数据源
     * @return
     */
    String dataSource() default "";
}
