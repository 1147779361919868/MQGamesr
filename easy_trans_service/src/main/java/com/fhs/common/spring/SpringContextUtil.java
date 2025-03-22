package com.fhs.common.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用于获取spring 的 ApplicationContext
 *
 * @Filename: SpringContextUtil.java
 * @Description:
 * @Version: 1.0
 * @Author: jackwang
 * @Email: wanglei@sxpartner.com
 * @History:<br> 陕西小伙伴网络科技有限公司
 * Copyright (c) 2017 All Rights Reserved.
 */
@Slf4j
public class SpringContextUtil implements ApplicationContextAware {
    private static ApplicationContext applicationContext = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.info("------SpringContextUtil setApplicationContext-------");
        SpringContextUtil.applicationContext = applicationContext;
    }

    public static void setStaticApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.info("------SpringContextUtil setApplicationContext-------");
        SpringContextUtil.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 注意 bean name默认 = 类名(首字母小写)
     * 例如: A8sClusterDao = getBean("k8sClusterDao")
     *
     * @param name
     * @return
     * @throws BeansException
     */
    public static Object getBean(String name) throws BeansException {
        return applicationContext.getBean(name);
    }

    /**
     * 根据类名获取到bean
     *
     * @param <T>
     * @param clazz
     * @return
     * @throws BeansException
     */
    public static <T> T getBeanByName(Class<T> clazz) throws BeansException {
        try {
            return applicationContext.getBean(clazz);
        } catch (Exception e) {
            log.error("获取对象错误:" + e.getMessage());
            return null;
        }
    }

    /**
     * 根据类名获取到bean(实际对象)
     *
     * @param <T>
     * @param clazz
     * @return
     * @throws BeansException
     */
    public static <T> T getBeanByClass(Class<T> clazz) throws BeansException {
        try {
            return (T) getTarget(applicationContext.getBean(clazz));
        } catch (Exception e) {
            log.error("获取对象错误:" + e.getMessage());
            return null;
        }
    }

    private static final Map<String, Object> CACHE_MAP = new HashMap<>();

    /**
     * 根据class 从beanfactory中找到对应的实现类集合
     *
     * @param clazz clazz
     * @param <T>   class的类型
     * @return beanfactory中找到对应的实现类集合
     */
    public static <T> List<T> getBeansByClass(Class<T> clazz) {
        String[] names = getApplicationContext().getBeanNamesForType(clazz);
        List<T> result = new ArrayList<>();
        for (String name : names) {
            result.add((T) SpringContextUtil.getBean(name));
        }
        return result;
    }

    /**
     * 获取微服务接口的实现类
     *
     * @param clazz 微服务接口
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBeanByClassForApi(Class<T> clazz) {
        String[] names = SpringContextUtil.getApplicationContext().getBeanNamesForType(clazz);
        T resultBean = null;
        String serviceName = clazz.getName();
        if (names.length > 1) {
            for (String name : names) {
                if (name.equals(serviceName)) {
                    continue;
                }
                resultBean = (T) SpringContextUtil.getBean(name);
                break;
            }
        }
        if (resultBean == null) {
            resultBean = (T) SpringContextUtil.getBean(serviceName);
        }
        return resultBean;
    }

    /**
     * 获取 目标对象
     *
     * @param proxy 代理对象
     * @return
     * @throws Exception
     */
    public static Object getTarget(Object proxy) {
        if (!AopUtils.isAopProxy(proxy)) {
            return proxy;//不是代理对象
        }

        if (AopUtils.isJdkDynamicProxy(proxy)) {
            try {
                proxy = getJdkDynamicProxyTargetObject(proxy);
            } catch (Exception e) {
                log.error("获取对象错误:" + e.getMessage());
                return proxy;
            }
        } else { //cglib
            try {
                proxy = getCglibProxyTargetObject(proxy);
            } catch (Exception e) {
                log.error("获取对象错误:" + e.getMessage());
                return proxy;
            }
        }
        return getTarget(proxy);
    }


    private static Object getCglibProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
        h.setAccessible(true);
        Object dynamicAdvisedInterceptor = h.get(proxy);
        Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        Object target = ((AdvisedSupport) advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget();
        return target;
    }


    private static Object getJdkDynamicProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getSuperclass().getDeclaredField("h");
        h.setAccessible(true);
        AopProxy aopProxy = (AopProxy) h.get(proxy);
        Field advised = aopProxy.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        Object target = ((AdvisedSupport) advised.get(aopProxy)).getTargetSource().getTarget();
        return target;
    }

    /**
     * 泛型注入
     *
     * @param clazz
     * @param actualTypeArguments
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBeanByClass(Class<T> clazz, String[] actualTypeArguments) {
        StringBuilder sBuilder = new StringBuilder();
        String cacheKey = null;
        for (String actualTypeArgument : actualTypeArguments) {
            sBuilder.append("_" + actualTypeArgument);
        }
        cacheKey = clazz.getName() + sBuilder.toString();
        if (CACHE_MAP.containsKey(cacheKey)) {
            return (T) CACHE_MAP.get(cacheKey);
        }
        // 获取候选人id
        String[] candidateNames = applicationContext.getBeanNamesForType(clazz);
        Object object = null;
        Type[] types = null;

        //遍历所有的候选人，看候选人的泛型
        for (String candidateName : candidateNames) {
            object = getBean(candidateName);

            if (object.getClass().getName().contains("EnhancerBySpringCGLIB")) {
                types = ((ParameterizedType) object.getClass().getSuperclass().getGenericSuperclass()).getActualTypeArguments();
            } else {
                types = ((ParameterizedType) object.getClass().getGenericSuperclass()).getActualTypeArguments();
            }
            boolean isThisObj = true;
            for (int i = 0; i < actualTypeArguments.length; i++) {

                if (!actualTypeArguments[i].equals(types[i].getTypeName())) {
                    isThisObj = false;
                    break;
                }

            }
            if (!isThisObj) {
                continue;
            }
            CACHE_MAP.put(cacheKey, object);
            return (T) object;
        }
        return null;
    }


    /**
     * 泛型注入
     *
     * @param clazz              clazz
     * @param actualTypeArgument 泛型类名
     * @param index              泛型的索引
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBeanByClass(Class<T> clazz, String actualTypeArgument, int index) {
        // 获取候选人id
        String[] candidateNames = applicationContext.getBeanNamesForType(clazz);
        Object object = null;
        Type[] types = null;
        //遍历所有的候选人，看候选人的泛型
        for (String candidateName : candidateNames) {
            object = getBean(candidateName);
            if (object.getClass().getName().contains("EnhancerBySpringCGLIB")) {
                types = ((ParameterizedType) object.getClass().getSuperclass().getGenericSuperclass()).getActualTypeArguments();
            } else {
                types = ((ParameterizedType) object.getClass().getGenericSuperclass()).getActualTypeArguments();

            }
            if (actualTypeArgument.equals(types[index].getTypeName())) {
                return (T) object;
            }
        }
        return null;
    }


    public static boolean containsBean(String name) {
        return applicationContext.containsBean(name);
    }

    public static boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        return applicationContext.isSingleton(name);
    }

}
