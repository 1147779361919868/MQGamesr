package com.fhs.trans.advice;

import com.fhs.core.trans.vo.VO;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.concurrent.TimeUnit;

/**
 * 释放缓存
 * @author wanglei
 */
@ControllerAdvice
public class ReleaseTransCacheAdvice{
    @ModelAttribute
    public void presetParam(Model model){
        Caffeine builder =  Caffeine.newBuilder();
        builder.expireAfterWrite(60, TimeUnit.SECONDS);
        VO.TRANS_MAP_CACHE.set(builder.build().asMap());
    }
}
