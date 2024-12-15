package com.fhs.trans.extend;

import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.AutoTransAble;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class JPATransableAdapter implements AutoTransAble {

    @Autowired
    private SessionFactory sessionFactory;

    private Class<? extends VO> voClass;

    public JPATransableAdapter(Class<? extends VO> voClass) {
        this.voClass = voClass;
    }


    @Override
    public List findByIds(List ids) {
        if(ids==null || ids.isEmpty()){
            return new ArrayList();
        }
        Query query = sessionFactory.getCurrentSession().createQuery(getSelectSql()
                +" WHERE tbl." + getPkeyFieldName() +   " IN (:ids)", voClass);
        query.setParameter("ids",ids);
        return query.list();
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
        Query query = sessionFactory.getCurrentSession().createQuery(getSelectSql(), voClass);
        return query.list();
    }

    @Override
    public VO selectById(Object pkey) {
        Query query = sessionFactory.getCurrentSession().createQuery(getSelectSql()
                +" WHERE tbl." + getPkeyFieldName() +   " = :id", voClass);
        query.setParameter("id",pkey);
        List<VO> result = query.list();
        if(result==null || result.isEmpty()){
           return null;
        }
        return result.get(0);
    }


}
