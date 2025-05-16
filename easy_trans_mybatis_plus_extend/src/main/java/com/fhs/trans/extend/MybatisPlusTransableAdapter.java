package com.fhs.trans.extend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.AutoTransAble;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MybatisPlusTransableAdapter implements AutoTransAble {

    private Class<? extends VO> voClass;

    public MybatisPlusTransableAdapter(Class<? extends VO> voClass) {
        this.voClass = voClass;
    }


    @Override
    public List findByIds(List ids) {
        Map<String, Object> map = new HashMap<>(1);
        map.put("coll", ids);
        SqlSession sqlSession = this.sqlSession();
        try {
            return sqlSession.selectList(this.sqlStatement(SqlMethod.SELECT_BATCH_BY_IDS), map);
        } finally {
            this.closeSqlSession(sqlSession);
        }
    }

    @Override
    public List select() {
        Map<String, Object> map = new HashMap<>(1);
        map.put("ew", new LambdaQueryWrapper<>());
        SqlSession sqlSession = this.sqlSession();
        try {
            return sqlSession.selectList(this.sqlStatement(SqlMethod.SELECT_LIST), map);
        } finally {
            this.closeSqlSession(sqlSession);
        }
    }

    @Override
    public VO selectById(Object primaryValue) {
        SqlSession sqlSession = this.sqlSession();
        VO result;
        try {
            result = (VO) sqlSession.selectOne(this.sqlStatement(SqlMethod.SELECT_BY_ID), primaryValue);
        } finally {
            this.closeSqlSession(sqlSession);
        }
        return result;
    }

    protected SqlSession sqlSession() {
        return SqlHelper.sqlSession(this.voClass);
    }

    protected String sqlStatement(SqlMethod sqlMethod) {
        return this.sqlStatement(sqlMethod.getMethod());
    }

    protected String sqlStatement(String sqlMethod) {
        return SqlHelper.table(voClass).getSqlStatement(sqlMethod);
    }

    protected void closeSqlSession(SqlSession sqlSession) {
        SqlSessionUtils.closeSqlSession(sqlSession, GlobalConfigUtils.currentSessionFactory(this.voClass));
    }
}
