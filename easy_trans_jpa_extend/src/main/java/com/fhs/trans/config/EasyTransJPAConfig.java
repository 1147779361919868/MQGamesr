package com.fhs.trans.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * mybatis plus适配器
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


}
