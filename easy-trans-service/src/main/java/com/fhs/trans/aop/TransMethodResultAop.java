package com.fhs.trans.aop;

import com.fhs.core.trans.anno.TransSett;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.TransService;
import com.fhs.trans.utils.TransUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 结果翻译aop
 *
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
        Set<String> includeFields = null;
        Set<String> excludeFields = null;
        try {
            proceed = joinPoint.proceed();
            //1.获取用户行为日志(ip,username,operation,method,params,time,createdTime)
            //获取类的字节码对象，通过字节码对象获取方法信息
            Class<?> targetCls = joinPoint.getTarget().getClass();
            //获取方法签名(通过此签名获取目标方法信息)
            MethodSignature ms = (MethodSignature) joinPoint.getSignature();
            //获取目标方法上的注解指定的操作名称
            Method targetMethod =
                    targetCls.getDeclaredMethod(
                            ms.getName(),
                            ms.getParameterTypes());
            if (targetMethod.isAnnotationPresent(TransSett.class)) {
                TransSett transSett = targetMethod.getAnnotation(TransSett.class);
                if (transSett.include().length != 0) {
                    includeFields = new HashSet<>(Arrays.asList(transSett.include()));
                }else{
                    excludeFields = new HashSet<>(Arrays.asList(transSett.exclude()));
                }
            }
        } catch (Throwable e) {
            throw e;
        }
        try {
            return TransUtil.transOne(proceed, transService, isEnableTile, new ArrayList<>(),includeFields,excludeFields);
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

