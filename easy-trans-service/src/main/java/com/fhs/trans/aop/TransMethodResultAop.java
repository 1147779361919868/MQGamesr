package com.fhs.trans.aop;

import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.TransService;
import com.fhs.trans.utils.TransUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 结果翻译aop
 * @author wanglei
 */
@Slf4j
@Aspect
public class TransMethodResultAop implements InitializingBean {

    /**
     * 开启平铺模式
     */
    @Value("${easy-trans.is-enable-tile:false}")
    private Boolean isEnableTile;


    /**
     * 支持vo包装类是map
     */
    @Value("${easy-trans.is-enable-map-result:false}")
    private Boolean isEnableMapResult;

    @Autowired
    private TransService transService;

    @Around("@annotation(com.fhs.core.trans.anno.TransMethodResult)")
    public Object transResult(ProceedingJoinPoint joinPoint) throws Throwable {
        Object proceed = null;
        try {
            proceed = joinPoint.proceed();
        } catch (Throwable e) {
            throw e;
        }
        try {
            return TransUtil.transOne(proceed, transService, isEnableTile, new ArrayList<>());
        } catch (Exception e) {
            log.error("翻译错误", e);
        }
        return proceed;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (isEnableMapResult) {
            TransUtil.transResultMap = true;
        }
    }
}

