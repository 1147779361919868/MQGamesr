package com.fhs.trans.advice;

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
        Object result = null;
        try {
            result = TransUtil.transOne(o,transService,isEnableTile);
        }catch (Exception e){
            log.error("翻译错误",e);
        }
        return result == null ? o : result;
    }
}
