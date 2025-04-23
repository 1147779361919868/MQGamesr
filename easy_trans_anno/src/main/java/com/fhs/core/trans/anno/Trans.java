package com.fhs.core.trans.anno;

import com.fhs.core.trans.vo.TransPojo;
import com.fhs.core.trans.vo.VO;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 翻译
 *
 * @author wanglei
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Trans {

    /**
     * 获取翻译类型，比如 wordbook 是字典
     *
     * @return 类型
     */
    String type();

    /**
     * 字段 比如  要翻译男女 上面的type写dictionary 此key写sex即可
     *
     * @return
     */
    String key() default "";

    /**
     * 设置到的target value  比如我有一个sex字段，有一个sexName 字段  sex是0 设置ref翻译服务可以自动把sexname设置为男
     * 如果是auto trans目标缓存有多少 有name,age 两个字段   我想要teacherName  可以写 teacherName#name
     *
     * @return
     */
    String ref() default "";

    /**
     * 目标class
     * @return
     */
    Class<? extends VO> target() default TransPojo.class;

    /**
     * 需要目标class哪些字段
     * @return
     */
    String[] fields() default {};

    /**
     * 别名
     * @return
     */
    String alias() default "";

    /**
     * 远程服务名称
     * @return
     */
    String serviceName() default  "";

    /**
     *
     * @return
     */
    String targetClassName() default "";
}
