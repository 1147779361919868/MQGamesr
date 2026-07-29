package com.fhs.trans.extend;

import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.SimpleTransService;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * mybatis plus 简单翻译驱动
 */
public class JPASimpleTransDiver implements SimpleTransService.SimpleTransDiver {
    private EntityManager em;

    public JPASimpleTransDiver(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<? extends VO> findByIds(List<? extends Serializable> ids, Class<? extends VO> targetClass,String uniqueField) {
        if(!StringUtils.isEmpty(uniqueField)){
            throw new RuntimeException("JPA不支持唯一索引来代替ID查询");
        }
        if (ids == null || ids.isEmpty()) {
            return new ArrayList();
        }
        TypedQuery query = em.createQuery(getSelectSql(targetClass)
                + " WHERE tbl." + getPkeyFieldName(targetClass) + " IN :ids", targetClass);
        query.setParameter("ids", ids);
        return query.getResultList();
    }

    @Override
    public VO findById(Serializable id, Class<? extends VO> targetClass,String uniqueField) {
        if(!StringUtils.isEmpty(uniqueField)){
            throw new RuntimeException("JPA不支持唯一索引来代替ID查询");
        }
        TypedQuery query = em.createQuery(getSelectSql(targetClass)
                + " WHERE tbl." + getPkeyFieldName(targetClass) + " = :id", targetClass);
        query.setParameter("id", id);
        try{
            return (VO) query.getSingleResult();
        }catch (NoResultException e) {
            return null;
        }
    }

    private String getSelectSql(Class<? extends VO> targetClass) {
        return "FROM " + targetClass.getSimpleName() + " AS tbl ";
    }

    private String getPkeyFieldName(Class<? extends VO> targetClass) {
        List<Field> fieldList = ReflectUtils.getAnnotationField(targetClass, javax.persistence.Id.class);
        if (fieldList == null || fieldList.isEmpty()) {
            throw new RuntimeException(targetClass.getName() + "没有@Id标记");
        }
        return fieldList.get(0).getName();
    }

}
