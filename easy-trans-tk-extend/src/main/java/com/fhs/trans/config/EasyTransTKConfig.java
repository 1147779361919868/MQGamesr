package com.fhs.trans.config;

import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.trans.extend.TKSimpleTransDiver;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.Id;

/**
 * TK Mybatis 适配器
 *
 * @author wanglei
 */
@Slf4j
@Configuration
@AutoConfigureAfter(
        name = {"tk.mybatis.mapper.autoconfigure.MapperAutoConfiguration"}
)
public class EasyTransTKConfig {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    @Bean
    public TKSimpleTransDiver tkSimpleTransDiver() {
        TKSimpleTransDiver result = new TKSimpleTransDiver();
        result.setSqlSessionFactory(sqlSessionFactory);
        result.setSqlSessionTemplate(sqlSessionTemplate);
        ReflectUtils.ID_ANNO.add(Id.class);
        return result;
    }
}
