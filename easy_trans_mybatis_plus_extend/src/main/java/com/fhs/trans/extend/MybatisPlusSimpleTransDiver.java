package com.fhs.trans.extend;

import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.SimpleTransService;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * mybatis plus 简单翻译驱动
 */
public class MybatisPlusSimpleTransDiver implements SimpleTransService.SimpleTransDiver {

    @Override
    public List<? extends VO> findByIds(List<? extends Serializable> ids, Class<? extends VO> targetClass) {
        Map<String, Object> map = CollectionUtils.newHashMapWithExpectedSize(1);
        map.put("coll", ids);
        SqlSession sqlSession = this.sqlSession(targetClass);
        try {
            return sqlSession.selectList(this.sqlStatement(SqlMethod.SELECT_BATCH_BY_IDS, targetClass), map);
        } finally {
            this.closeSqlSession(sqlSession, targetClass);
        }
    }

    @Override
    public VO findById(Serializable id, Class<? extends VO> targetClass) {
        SqlSession sqlSession = this.sqlSession(targetClass);
        VO result;
        try {
            result = (VO) sqlSession.selectOne(this.sqlStatement(SqlMethod.SELECT_BY_ID, targetClass), id);
        } finally {
            this.closeSqlSession(sqlSession, targetClass);
        }
        return result;
    }

    protected SqlSession sqlSession(Class<? extends VO> voClass) {
        return SqlHelper.sqlSession(voClass);
    }

    protected String sqlStatement(SqlMethod sqlMethod, Class<? extends VO> voClass) {
        return this.sqlStatement(sqlMethod.getMethod(), voClass);
    }

    protected String sqlStatement(String sqlMethod, Class<? extends VO> voClass) {
        return SqlHelper.table(voClass).getSqlStatement(sqlMethod);
    }

    protected void closeSqlSession(SqlSession sqlSession, Class<? extends VO> voClass) {
        SqlSessionUtils.closeSqlSession(sqlSession, GlobalConfigUtils.currentSessionFactory(voClass));
    }


}
