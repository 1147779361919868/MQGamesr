package com.fhs.trans.config;

import com.baomidou.mybatisplus.annotation.TableId;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.trans.extend.MybatisPlusSimpleTransDiver;
import com.fhs.trans.extend.MybatisPlusTransableRegister;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * mybatis plus适配器
 *
 * @author wanglei
 */
@Slf4j
@Configuration
public class EasyTransMybatisPlusConfig {

    /**
     * service的包路径
     */
    @Value("${easy-trans.autotrans.package:com.*.*.service.impl}")
    private String packageNames;

    @Bean
    public MybatisPlusTransableRegister mybatisPlusTransableRegister() {
        MybatisPlusTransableRegister result = new MybatisPlusTransableRegister();
        result.setPackageNames(packageNames);
        return result;
    }

    @Bean
    @Primary
    public MybatisPlusSimpleTransDiver MybatisPlusSimpleTransDiver() {
        ReflectUtils.ID_ANNO.add(TableId.class);
        return new MybatisPlusSimpleTransDiver();
    }
}
