package com.fhs.trans.advice;

import com.fhs.core.trans.anno.IgnoreTrans;
import com.fhs.core.trans.anno.TransSett;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.TransService;
import com.fhs.trans.utils.TransUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.*;

/**
 * 全局翻译实现
 *
 * @author wanglei
 */
@Slf4j
@ControllerAdvice
public class EasyTransResponseBodyAdvice implements ResponseBodyAdvice {

    /**
     * 开启平铺模式
     */
    @Value("${easy-trans.is-enable-tile:false}")
    private Boolean isEnableTile;

    @Autowired
    private TransService transService;

    @Override
    public boolean supports(MethodParameter methodParameter, Class aClass) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object o, MethodParameter methodParameter, MediaType mediaType, Class aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        // 如果主动指定了忽略某个方法，则不执行翻译
        if (methodParameter.getExecutable().isAnnotationPresent(IgnoreTrans.class)) {
            return o;
        }
        Set<String> includeFields = null;
        Set<String> excludeFields = null;
        if (methodParameter.getExecutable().isAnnotationPresent(TransSett.class)) {
            TransSett transSett = methodParameter.getExecutable().getAnnotation(TransSett.class);
            if (transSett.include().length != 0) {
                includeFields = new HashSet<>(Arrays.asList(transSett.include()));
            } else {
                excludeFields = new HashSet<>(Arrays.asList(transSett.exclude()));
            }
        }
        Object result = null;
        try {
            result = TransUtil.transOne(o, transService, isEnableTile, new ArrayList<>(), includeFields, excludeFields);
        } catch (Exception e) {
            log.error("翻译错误", e);
        }
        return result == null ? o : result;
    }
}
