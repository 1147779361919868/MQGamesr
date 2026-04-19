package com.fhs.trans.extend;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.*;
import com.baomidou.mybatisplus.core.toolkit.support.ColumnCache;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.SimpleTransService;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * mybatis plus 简单翻译驱动
 */
public class MybatisPlusSimpleTransDiver implements SimpleTransService.SimpleTransDiver {

    @Override
    public List<? extends VO> findByIds(List<? extends Serializable> ids, Class<? extends VO> targetClass, String uniqueField) {
        SqlSession sqlSession = this.sqlSession(targetClass);
        try {
            //没指定唯一键 则使用主键
            if(StringUtils.isBlank(uniqueField)){
                return getMapper(targetClass, sqlSession).selectBatchIds(ids);
            }
            //指定唯一键 则使用唯一键
            QueryWrapper queryWrapper = new QueryWrapper<>();
            queryWrapper.in(getColumn(targetClass, uniqueField), ids);
            return getMapper(targetClass, sqlSession).selectList(queryWrapper);
        } finally {
            this.closeSqlSession(sqlSession, targetClass);
        }
    }

    @Override
    public VO findById(Serializable id, Class<? extends VO> targetClass, String uniqueField) {
        SqlSession sqlSession = this.sqlSession(targetClass);
        try {
            //没指定唯一键 则使用主键
            if(StringUtils.isBlank(uniqueField)){
                return getMapper(targetClass, sqlSession).selectById(id);
            }
            //指定唯一键 则使用唯一键
            QueryWrapper queryWrapper = new QueryWrapper<>();
            queryWrapper.eq(getColumn(targetClass, uniqueField), id);
            return getMapper(targetClass, sqlSession).selectOne(queryWrapper);
        } finally {
            this.closeSqlSession(sqlSession, targetClass);
        }
    }

    public String getColumn(Class<? extends VO> targetClass, String field) {
        Map<String, ColumnCache> cacheMap = LambdaUtils.getColumnMap(targetClass);
        if (cacheMap == null) {
            ExceptionUtils.mpe("Can not find TableInfo from Class: \"%s\".", targetClass.getName());
        }
        if (!cacheMap.containsKey(field.toUpperCase())) {
            ExceptionUtils.mpe("Can not find field: \"%s\" in Class: \"%s\".", field, targetClass.getName());
        }
        return cacheMap.get(field.toUpperCase()).getColumn();
    }

    protected SqlSession sqlSession(Class<? extends VO> voClass) {
        return SqlHelper.sqlSession(voClass);
    }

    protected String sqlStatement(String sqlMethod, Class<? extends VO> voClass) {
        return SqlHelper.table(voClass).getSqlStatement(sqlMethod);
    }

    protected void closeSqlSession(SqlSession sqlSession, Class<? extends VO> voClass) {
        SqlSessionUtils.closeSqlSession(sqlSession, GlobalConfigUtils.currentSessionFactory(voClass));
    }

    private BaseMapper<? extends VO> getMapper(Class<? extends VO> entityClass, SqlSession sqlSession) {
        Optional.ofNullable(entityClass).orElseThrow(() -> ExceptionUtils.mpe("entityClass can't be null!"));
        TableInfo tableInfo = Optional.ofNullable(TableInfoHelper.getTableInfo(entityClass)).orElseThrow(() -> ExceptionUtils.mpe("Can not find TableInfo from Class: \"%s\".", entityClass.getName()));
        try {
            Configuration configuration = (Configuration)ReflectUtils.getValue(tableInfo, "configuration");
            return (BaseMapper<? extends VO>) configuration.getMapper(Class.forName(tableInfo.getCurrentNamespace()), sqlSession);
        } catch (ClassNotFoundException e) {
            throw ExceptionUtils.mpe(e);
        }
    }


}
