package com.fhs.trans.extend;

import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.AutoTransAble;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class JPATransableAdapter implements AutoTransAble {

    private EntityManager em;

    private Class<? extends VO> voClass;

    public JPATransableAdapter(Class<? extends VO> voClass, EntityManager em) {
        this.em = em;
        this.voClass = voClass;
    }


    @Override
    public List findByIds(List ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList();
        }
        TypedQuery query = em.createQuery(getSelectSql()
                + " WHERE tbl." + getPkeyFieldName() + " IN :ids", voClass);
        query.setParameter("ids", ids);
        return query.getResultList();
    }

    private String getSelectSql() {
        return "FROM " + voClass.getSimpleName() + " AS tbl ";
    }

    private String getPkeyFieldName() {
        List<Field> fieldList = ReflectUtils.getAnnotationField(voClass, javax.persistence.Id.class);
        if (fieldList == null || fieldList.isEmpty()) {
            throw new RuntimeException(voClass.getName() + "没有@Id标记");
        }
        return fieldList.get(0).getName();
    }

    @Override
    public List select() {
        TypedQuery query = em.createQuery(getSelectSql(), voClass);
        return query.getResultList();
    }

    @Override
    public VO selectById(Object pkey) {
        TypedQuery query = em.createQuery(getSelectSql()
                + " WHERE tbl." + getPkeyFieldName() + " = :id", voClass);
        query.setParameter("id", pkey);
        return (VO) query.getSingleResult();
    }


}
