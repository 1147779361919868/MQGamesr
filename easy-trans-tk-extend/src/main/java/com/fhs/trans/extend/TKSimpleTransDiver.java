package com.fhs.trans.extend;

import com.fhs.common.spring.SpringContextUtil;
import com.fhs.common.utils.StringUtil;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.SimpleTransService;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.entity.EntityColumn;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TK mybatis 简单翻译驱动
 */
@Slf4j
public class TKSimpleTransDiver  extends SqlSessionDaoSupport implements SimpleTransService.SimpleTransDiver , ApplicationListener<ApplicationReadyEvent> {

    /**
     * key 是entityclass value是mapperclass的名字方便拼接sqlid
     */
    private Map<Class,Mapper> entityMapperMap = new HashMap<>();

    /**
     * key 是entityclass value是mapperclass的方便拼接sqlid
     */
    private Map<Class,Class> entityMapperClassMap = new HashMap<>();

    public TKSimpleTransDiver() {
    }

    @Override
    public List<? extends VO> findByIds(List<? extends Serializable> ids, Class<? extends VO> targetClass,String uniqueField) {
        Example example = new Example(targetClass);
        if(StringUtil.isEmpty(uniqueField)){
            Set<EntityColumn> columnList = EntityHelper.getPKColumns(targetClass);
            if (columnList.size() == 1) {
                EntityColumn column = columnList.iterator().next();
                example.createCriteria().andIn(column.getProperty(),ids);
            }else {
                throw new IllegalArgumentException (targetClass + "没有配置id或者配置了多个id 属性");
            }
        }else{
            example.createCriteria().andIn(uniqueField,ids);
        }
        return getMapper(targetClass).selectByExample(example);
    }

    @Override
    public VO findById(Serializable id, Class<? extends VO> targetClass,String uniqueField) {
        if(StringUtil.isEmpty(uniqueField)){
            return (VO) getMapper(targetClass).selectByPrimaryKey(id);
        }
        Example example = new Example(targetClass);
        example.createCriteria().andEqualTo(uniqueField,id);
        return (VO) getMapper(targetClass).selectOneByExample(example);
    }

    @Override
    public org.apache.ibatis.session.SqlSession getSqlSession() {
        return super.getSqlSession();
    }

    /**
     * 根据实体类获取mapper
     * @param entity  实体类
     * @return
     */
    public Mapper getMapper(Class entity){
        if(entityMapperMap.containsKey(entity)){
            return entityMapperMap.get(entity);
        }
        throw new IllegalArgumentException (entity + "找不到对应的mapper，请检查");
    }

    /**
     * 根据
     * @param entityClazz
     * @return
     */
    public Class<? extends Mapper> getMapperClass(Class entityClazz){
        return entityMapperClassMap.get(entityClazz);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        //系统启动成功后获取所有的mapper 建立 entity和mapper的映射关系
        List<Mapper> mappers = SpringContextUtil.getBeansByClass(Mapper.class);
        Mapper mapper = null;
        for (int i = 0; i < mappers.size(); i++) {
            mapper = mappers.get(i);
            Field h = null;
            try {
                if (AopUtils.isJdkDynamicProxy(mapper)) {{
                    mapper = (Mapper) getJdkDynamicProxyTargetObject(mapper);
                }}
                h = mapper.getClass().getSuperclass().getDeclaredField("h");
                h.setAccessible(true);
                Object mapperProxy = h.get(mapper);
                Class mapperClass = (Class) ReflectUtils.getValue(mapperProxy, "mapperInterface");
                Type[] types = mapperClass.getGenericInterfaces();
                for (Type type : types) {
                    if (type instanceof ParameterizedType) {
                        ParameterizedType t = (ParameterizedType) type;
                        Class<?> entityClass = (Class<?>) t.getActualTypeArguments()[0];
                        entityMapperMap.put(entityClass,mapper);
                        entityMapperClassMap.put(entityClass,mapperClass);
                    }
                }
            } catch (Exception e) {
                log.warn("mapper and  entity relation parse error,Mapper:" + mapper);
            }
        }
    }


    private  Object getJdkDynamicProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getSuperclass().getDeclaredField("h");
        h.setAccessible(true);
        AopProxy aopProxy = (AopProxy) h.get(proxy);
        Field advised = aopProxy.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        Object target = ((AdvisedSupport) advised.get(aopProxy)).getTargetSource().getTarget();
        return target;
    }
}
