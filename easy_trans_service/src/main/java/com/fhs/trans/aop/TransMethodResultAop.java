package com.fhs.trans.aop;

import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.TransService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 结果翻译aop
 */
@Slf4j
@Aspect
public class TransMethodResultAop {

    @Autowired
    private TransService transService;

    @Around("@annotation(com.fhs.core.trans.anno.TransMethodResult)")
    public Object transResult(ProceedingJoinPoint joinPoint) throws Throwable {
        Object proceed = null;
        try {
            proceed = joinPoint.proceed();
            List<VO> transOneVOs = getTransOneVOs(proceed);
            for (VO transOneVO : transOneVOs) {
                transService.transOne(transOneVO);
            }
            List<List<VO>> voLists = getTransMoreVOS(proceed);
            for (List<VO> voList : voLists) {
                transService.transMore(voList);
            }
            return proceed;
        } catch (Throwable e) {
            throw e;
        }
    }


    /**
     * 获取单个vo
     *
     * @param proceed
     * @return
     */
    private List<VO> getTransOneVOs(Object proceed) {
        List<VO> result = new ArrayList<>();
        if (proceed == null) {
            return result;
        }
        if (VO.class.isAssignableFrom(proceed.getClass())) {
            result.add((VO) proceed);
        }
        List<Field> fields = ReflectUtils.getAllField(proceed);
        VO vo = null;
        for (Field field : fields) {
            if (checkFieldIsVO(field, proceed)) {
                try {
                    field.setAccessible(true);
                    vo = (VO) field.get(proceed);
                    if (vo != null) {
                        result.add(vo);
                    }
                } catch (IllegalAccessException e) {
                    log.error("获取值错误", e);
                }
            }
        }

        return result;
    }

    /**
     * 判断一个是否vo
     *
     * @param field
     * @return
     */
    public boolean checkFieldIsVO(Field field, Object proceed) {
        Class clazz = getFieldClass(field, proceed);
        if (clazz == null) {
            return false;
        }
        if (VO.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }


    /**
     * 获取trans more vos
     *
     * @param proceed
     * @return
     */
    private List<List<VO>> getTransMoreVOS(Object proceed) {
        List<List<VO>> result = new ArrayList<>();
        if (proceed == null) {
            return result;
        }
        if (Collection.class.isAssignableFrom(proceed.getClass())) {
            result.add(new ArrayList<>((Collection) proceed));
        }
        List<Field> fields = ReflectUtils.getAllField(proceed);
        List voList = null;
        for (Field field : fields) {
            if (checkFieldIsCollection(field, proceed)) {
                field.setAccessible(true);
                try {
                    Collection collection = (Collection) field.get(proceed);
                    if (collection == null || collection.isEmpty()) {
                        continue;
                    }
                    voList = new ArrayList();
                    voList.addAll(collection);
                    result.add(voList);
                } catch (IllegalAccessException e) {
                    log.error("", e);
                }
            }
        }
        return result;
    }

    public Class getFieldClass(Field field, Object proceed) {
        String className = field.getGenericType().getTypeName().replace("class ", "");
        if ("T".equals(className)) {
            try {
                field.setAccessible(true);
                Object obj = field.get(proceed);
                if (obj == null) {
                    return Object.class;
                }
                if (VO.class.isAssignableFrom(obj.getClass())) {
                    return DefaultVO.class;
                }
                if (Collection.class.isAssignableFrom(obj.getClass())) {
                    return List.class;
                }
            } catch (IllegalAccessException e) {
                log.error("", e);
                return Object.class;
            }
        }
        //不支持数组 基础数据类型和内部类
        if (className.contains("<")) {
            className = className.substring(0, className.indexOf("<"));
        }
        //不支持数组 基础数据类型和内部类
        if (className.contains("/") ||
                (Character.isLowerCase(className.charAt(0)) && (!className.contains(".")))
                || className.contains("$")) {
            return null;
        }

        if (className.contains("]")) {
            className = className.substring(0, className.indexOf("["));
        }
        if (className.contains("]")) {
            className = className.substring(0, className.indexOf("<"));
        }

        try {
            Class clazz = Class.forName(className);
            return clazz;
        } catch (ClassNotFoundException e) {
            log.error("", e);
        }
        return null;
    }

    /**
     * 判断一个字段是否是集合
     *
     * @param field
     * @return
     */
    public boolean checkFieldIsCollection(Field field, Object proceed) {
        Class clazz = getFieldClass(field, proceed);
        if (clazz == null) {
            return false;
        }
        if (Collection.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }
}

/**
 * 实际上没什么用，只是用来判断T是否是VO的时候返回此类型
 */
class DefaultVO implements VO {

    @Override
    public Map<String, String> getTransMap() {
        return null;
    }
}
