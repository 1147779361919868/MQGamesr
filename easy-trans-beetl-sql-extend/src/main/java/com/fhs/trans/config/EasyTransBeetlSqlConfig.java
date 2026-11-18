package com.fhs.trans.config;

import com.fhs.core.trans.vo.VO;
import com.fhs.trans.extend.BeetlSqlTransDiver;
import lombok.extern.slf4j.Slf4j;
import org.beetl.sql.annotation.entity.AssignID;
import org.beetl.sql.core.SQLManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TK Mybatis 适配器
 *
 * @author wanglei
 */
@Slf4j
@Configuration
public class EasyTransBeetlSqlConfig implements InitializingBean {

    @Autowired
    private SQLManager sqlManager;

    @Bean
    public BeetlSqlTransDiver beetlSqlTransDiver() {
        BeetlSqlTransDiver result = new BeetlSqlTransDiver(sqlManager);
        return result;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        VO.ID_ANNO.add(AssignID.class);
    }
}
