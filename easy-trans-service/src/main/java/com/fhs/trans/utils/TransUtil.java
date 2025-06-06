package com.fhs.trans.utils;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fhs.common.utils.ConverterUtils;
import com.fhs.common.utils.StringUtil;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.TransService;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TransUtil {

    /**
     * 是否翻译map
     * 有一些框架的vo封装类 是一个map，为了适配加个开关，打开性能有影响
     */
    public static boolean transResultMap = false;


    private static Map<Class, Class> proxyClassMap = new ConcurrentHashMap<>();

    private static Map<Class, Set<String>> proxyClassFieldMap = new ConcurrentHashMap<>();

    /**
     * 翻译集合
     *
     * @param object       被翻译的对象
     * @param transService 翻译服务
     * @param isProxy      是否创建代理
     * @return
     */
    public static Collection transBatch(Object object, TransService transService, boolean isProxy, ArrayList<Object> hasTransObjs,
                                        Set<String> includeFields, Set<String> excludeFields) throws IllegalAccessException, InstantiationException {
        Collection param = (Collection) object;
        if (param == null) {
            return null;
        }
        if (param.isEmpty() || param.iterator().next() == null) {
            return param;
        }
        boolean isVo = false;
        //防止二次翻译
        if (param.iterator().next().getClass().getName().contains("DynamicTypeBuilder")) {
            return param;
        }
        if (param.iterator().next() instanceof VO) {
            transService.transMore(new ArrayList<>(param), includeFields, excludeFields);
            for (Object tempObject : param) {
                transFields(tempObject, transService, isProxy, hasTransObjs, includeFields, excludeFields);
            }
            //vo 嵌套vo的时候不做
            isVo = true;
        } else {
            for (Object tempObject : param) {
                transOne(tempObject, transService, isProxy, hasTransObjs, includeFields, excludeFields);
            }
        }
        if (!isProxy || (!isVo)) {
            return (Collection) object;
        }
        Collection result = null;
        //暂时只支持list和set的子类 ，其他的集合暂时不支持
        if (param instanceof List) {
            result = new ArrayList();
        } else if (param instanceof Set) {
            result = new HashSet();
        } else {
            return param;
        }
        for (Object vo : param) {
            if (contains(hasTransObjs, vo)) {
                continue;
            }
            hasTransObjs.add(vo);
            result.add(createProxyVo((VO) vo));
        }
        return result;
    }

    /**
     * 合并需要翻译的对象到集合
     *
     * @param vo    vo
     * @param vos   vos
     * @param voMap vomap
     */
    public static void mergeTransSubVo(VO vo, Collection<? extends VO> vos, Map<Class, List<? extends VO>> voMap) {
        List hasVoList = null;
        if (vo == null) {
            vo = vos.iterator().next();
        }
        hasVoList = voMap.containsKey(vo.getClass()) ? voMap.get(vo.getClass()) : new ArrayList<>();
        if (vos != null) {
            hasVoList.addAll(vos);
        } else {
            hasVoList.add(vo);
        }
        voMap.put(vo.getClass(), hasVoList);
    }

    /**
     * 判断是否包含某个对象
     *
     * @param list 对象集合
     * @param obj  对象
     * @return true 包含 false 不包含
     */
    private static boolean contains(List<Object> list, Object obj) {
        for (Object o : list) {
            if (o == obj) {
                return true;
            }
        }
        return false;
    }

    /**
     * 翻译单个对象
     *
     * @param object       被翻译的对象
     * @param transService 翻译服务
     * @param isProxy      是否启用代理
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static Object transOne(Object object, TransService transService, boolean isProxy
            , ArrayList<Object> hasTransObjs, Set<String> includeFields, Set<String> excludeFields) throws IllegalAccessException, InstantiationException {
        if (object == null) {
            return null;
        }
        if (contains(hasTransObjs, object)) {
            return object;
        }
        hasTransObjs.add(object);
        boolean isVo = false;
        //如果要添加map翻译支持
        if (transResultMap) {
            if (object instanceof Map) {
                Map tempMap = (Map) object;
                for (Object key : tempMap.keySet()) {
                    Object mapValue = tempMap.get(key);
                    try {
                        tempMap.put(key, transOne(mapValue, transService, isProxy, hasTransObjs, includeFields, excludeFields));
                    } catch (Exception e) {
                        //因为有一些map不允许修改，导致异常，不用处理就可以，最多平铺不成功。
                    }
                }
                return object;
            }
        }
        if (object instanceof VO) {
            //代理对象不重新翻译
            if (object.getClass().getName().contains("DynamicTypeBuilder")) {
                return object;
            }
            transService.transOne((VO) object, includeFields, excludeFields);
            transFields(object, transService, isProxy, hasTransObjs, includeFields, excludeFields);
            isVo = true;
        } else if (object instanceof Collection) {
            return transBatch(object, transService, isProxy, hasTransObjs, includeFields, excludeFields);
        } else if (object.getClass().getName().startsWith("java.")) {
            return object;
        } else {

            transFields(object, transService, isProxy, hasTransObjs, includeFields, excludeFields);
        }
        return (isProxy && isVo) ? createProxyVo((VO) object) : object;
    }


    /**
     * 翻译一个object的子属性
     *
     * @param object
     * @param transService
     * @param isProxy
     * @param hasTransObjs
     */
    public static void transFields(Object object, TransService transService, boolean isProxy, ArrayList<Object> hasTransObjs
            , Set<String> includeFields, Set<String> excludeFields) throws IllegalAccessException, InstantiationException {
        List<Field> fields = ReflectUtils.getAllField(object);
        Object tempObj = null;
        for (Field field : fields) {
            // 此段代码适配beanSearcher SearchResult
            if (java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                field.setAccessible(true);
                tempObj = field.get(object);
                if (Objects.nonNull(tempObj) && tempObj instanceof List) {
                    List tempList = (List) tempObj;
                    if (!CollectionUtils.isEmpty(tempList) && Objects.nonNull(tempList.get(0)) && tempList.get(0) instanceof VO) {
                        Collection transResult = transBatch(tempObj, transService, isProxy, hasTransObjs, includeFields, excludeFields);
                        tempList.clear();
                        tempList.addAll(transResult);
                    }
                }
            }
            if (java.lang.reflect.Modifier.isFinal(field.getModifiers()) || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            tempObj = field.get(object);
            try {
                field.set(object, transOne(tempObj, transService, isProxy, hasTransObjs, includeFields, excludeFields));
            } catch (Exception e) {
                log.error("如果字段set错误，请反馈给easytrans开发者", e);
            }

        }
    }


    /**
     * 校验属性是否在class中都存在
     *
     * @param propertes
     * @param clazz
     * @return
     */
    private static boolean validProxyClass(Set<String> propertes, Class clazz) {
        Set<String> fieldSet = proxyClassFieldMap.get(clazz);
        return fieldSet.containsAll(propertes);
    }


    /**
     * 生成新class
     *
     * @param vo vo
     * @return
     */
    public static Class genNewClass(VO vo) {
        try {
            Class targetClass = proxyClassMap.get(vo.getClass());
            boolean isGenNewClass = true;
            // 如果之前生成过class 并且不缺少字段则不重新生成class
            if (targetClass != null && validProxyClass(vo.getTransMap().keySet(), targetClass)) {
                isGenNewClass = false;
            }
            if (isGenNewClass) {
                AnnotationDescription jacksonIgnore = AnnotationDescription.Builder.ofType(JsonIgnore.class)
                        .build();
                AnnotationDescription fastJsonIgnore = AnnotationDescription.Builder.ofType(JSONField.class)
                        .define("serialize", false)
                        .build();
                DynamicType.Builder<? extends Object> builder = new ByteBuddy()
                        .subclass(vo.getClass())
                        .name(vo.getClass().getSimpleName() + "DynamicTypeBuilder" + StringUtil.getUUID())
                        .defineMethod("getTransMap", Map.class, Modifier.PUBLIC)
                        .intercept(FixedValue.nullValue())
                        .annotateMethod(jacksonIgnore, fastJsonIgnore);
                for (String property : vo.getTransMap().keySet()) {
                    //添加属性
                    builder = builder.defineField(property, String.class, Modifier.PUBLIC);
                }

                targetClass = builder.make()
                        .load(ClassUtils.getDefaultClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                        .getLoaded();
                proxyClassMap.put(vo.getClass(), targetClass);
                proxyClassFieldMap.put(targetClass, vo.getTransMap().keySet());
            }
            return targetClass;
        } catch (Exception e) {
            if (e instanceof ClassNotFoundException) {
                log.error("生成新class错误，目前不支持JDK17，请关闭平铺模式: easy-trans.is-enable-tile 设置为false");
            } else {
                log.error("生成新class错误", e);
            }

        }
        return null;
    }

    /**
     * 创建新 vo
     *
     * @param vo
     * @return
     */
    public static Object createProxyVo(VO vo) {
        if (vo == null || vo.getTransMap() == null) {
            return vo;
        }
        try {
            Class clazz = genNewClass(vo);
            Object newObject = clazz.newInstance();
            if (newObject == null) {
                return vo;
            }
            BeanUtils.copyProperties(vo, newObject);
            for (String property : vo.getTransMap().keySet()) {
                ReflectUtils.setValue(newObject, property, ConverterUtils.toString(vo.getTransMap().get(property)));
            }
            return newObject;
        } catch (Exception e) {
            log.error("easy trans 赋值错误", e);
        }
        return vo;
    }


}

