package com.fhs.trans.config;

import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.trans.extend.JPASimpleTransDiver;
import com.fhs.trans.extend.JPATransableRegister;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.persistence.EntityManager;
import javax.persistence.Id;

/**
 * JPA适配器
 *
 * @author wanglei
 */
@Slf4j
@Configuration
public class EasyTransJPAConfig {

    /**
     * service的包路径
     */
    @Value("${easy-trans.autotrans.package:com.*.*.service.impl}")
    private String packageNames;

    @Bean
    public JPATransableRegister jpaTransableRegister() {
        JPATransableRegister result = new JPATransableRegister();
        result.setPackageNames(packageNames);
        return result;
    }

    @Bean
    public JPASimpleTransDiver jpaSimpleTransDiver(EntityManager em) {
        ReflectUtils.ID_ANNO.add(Id.class);
        JPASimpleTransDiver result = new JPASimpleTransDiver(em);
        return result;
    }

}
