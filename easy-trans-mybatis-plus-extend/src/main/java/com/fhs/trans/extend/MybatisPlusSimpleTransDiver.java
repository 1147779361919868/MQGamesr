package com.fhs.trans.extend;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.*;
import com.baomidou.mybatisplus.core.toolkit.support.ColumnCache;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.fhs.common.utils.StringUtil;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * mybatis plus 简单翻译驱动
 */
public class MybatisPlusSimpleTransDiver implements SimpleTransService.SimpleTransDiver {

    @Override
    public List<? extends VO> findByIds(List<? extends Serializable> ids, Class<? extends VO> targetClass, String uniqueField) {
        return findByIds(ids, targetClass, uniqueField, null);
    }

    @Override
    public List<? extends VO> findByIds(List<? extends Serializable> ids, Class<? extends VO> targetClass, String uniqueField, Set<String> targetFields) {
        SqlSession sqlSession = this.sqlSession(targetClass);
        try {
            uniqueField = this.getUniqueField(targetClass, uniqueField);
            //指定唯一键 则使用唯一键
            QueryWrapper queryWrapper = genWrapper(targetClass, targetFields,uniqueField);
            queryWrapper.in(getColumn(targetClass, uniqueField), ids);
            return getMapper(targetClass, sqlSession).selectList(queryWrapper);
        } finally {
            this.closeSqlSession(sqlSession, targetClass);
        }
    }

    @Override
    public VO findById(Serializable id, Class<? extends VO> targetClass, String uniqueField) {
        return findById(id, targetClass, uniqueField, null);
    }

    @Override
    public VO findById(Serializable id, Class<? extends VO> targetClass, String uniqueField, Set<String> targetFields) {
        SqlSession sqlSession = this.sqlSession(targetClass);
        try {
            uniqueField = this.getUniqueField(targetClass, uniqueField);
            //指定唯一键 则使用唯一键
            QueryWrapper queryWrapper = genWrapper(targetClass, targetFields,uniqueField);
            queryWrapper.eq(getColumn(targetClass, uniqueField), id);
            return getMapper(targetClass, sqlSession).selectOne(queryWrapper);
        } finally {
            this.closeSqlSession(sqlSession, targetClass);
        }
    }

    /**
     * 生成一个wrapper
     *
     * @param targetClass
     * @param targetFields
     * @return
     */
    private QueryWrapper genWrapper(Class<? extends VO> targetClass, Set<String> targetFields, String uniqueField) {
        //指定唯一键 则使用唯一键
        QueryWrapper queryWrapper = new QueryWrapper<>();
        // 如果指定了字段就指定字段
        if (targetFields != null && !targetFields.isEmpty()) {
            targetFields.add(getKeyProperty(targetClass));
            if (!StringUtil.isEmpty(uniqueField)) {
                targetFields.add(uniqueField);
            }
            queryWrapper.select(targetFields.stream().map(column -> {
                return this.getColumn(targetClass, column);
            }).toArray(String[]::new));
        }
        return queryWrapper;
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

    /**
     * 如果配置了uniqueField 则返回uniqueField 没有就返回主键
     *
     * @param targetClass 目标class
     * @param uniqueField 唯一键
     * @return
     */
    private String getUniqueField(Class<? extends VO> targetClass, String uniqueField) {
        if (!StringUtil.isEmpty(uniqueField)) {
            return uniqueField;
        }
        return getKeyProperty(targetClass);
    }

    /**
     * 获取主键属性
     *
     * @param targetClass po类
     * @return 主键属性
     */
    private String getKeyProperty(Class<? extends VO> targetClass) {
        TableInfo tableInfo = Optional.ofNullable(TableInfoHelper.getTableInfo(targetClass)).orElseThrow(() -> ExceptionUtils.mpe("Can not find TableInfo from Class: \"%s\".", targetClass.getName()));
        return tableInfo.getKeyProperty();
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
            Configuration configuration = (Configuration) ReflectUtils.getValue(tableInfo, "configuration");
            return (BaseMapper<? extends VO>) configuration.getMapper(Class.forName(tableInfo.getCurrentNamespace()), sqlSession);
        } catch (ClassNotFoundException e) {
            throw ExceptionUtils.mpe(e);
        }
    }


}
