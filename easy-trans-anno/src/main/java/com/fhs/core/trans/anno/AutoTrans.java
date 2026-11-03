package com.fhs.core.trans.anno;

import com.fhs.core.trans.vo.VO;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Description: 此注解可标记到AutoTransAble的实现类或者jpa的到或者mybatis plus的mapper上
 * @Author: wanglei
 * @Date: Created in 10:14 2019/10/15
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface AutoTrans {
    /**
     * 命名空间
     *
     * @return
     */
    String namespace();

    /**
     * 字段集合
     *
     * @return
     */
    String[] fields();

    /**
     * 是否使用缓存翻译
     *
     * @return 默认为true 如果是false的话
     */
    boolean useCache() default false;

    /**
     * 是否使用redis存放缓存
     *
     * @return 默认false
     */
    boolean useRedis() default false;

    /**
     * 默认的别名
     *
     * @return
     */
    String defaultAlias() default "";

    /**
     * 关联的类  jpa dao/mbatis plus的时候mapper专用参数
     *
     * @return
     */
    Class<? extends VO> ref() default VO.class;

    /**
     * 全局缓存
     * @return
     */
    boolean globalCache() default false;

    /**
     * globalCache 为true有效
     * true 代表按照访问时间过期
     * false 按照插入时间过期
     * @return
     */
    boolean isAccess() default false;

    /**
     *  globalCache 为true有效
     *  缓存时间 到秒
     * @return
     */
    long cacheSeconds() default 1l;

    /**
     * globalCache 为true有效
     * 最大缓存多少个
     * @return
     */
    int maxCache() default 1000;

}
