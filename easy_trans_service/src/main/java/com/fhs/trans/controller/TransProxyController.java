package com.fhs.trans.controller;

import com.fhs.common.utils.ConverterUtils;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.SimpleTransService;
import com.fhs.trans.vo.BasicVO;
import com.fhs.trans.vo.FindByIdsQueryPayload;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 翻译服务请求转发代理
 */
@Data
@Slf4j
@RestController
@RequestMapping("/easyTrans/proxy")
public class TransProxyController {


    private SimpleTransService.SimpleTransDiver simpleTransDiver;

    /**
     * findByIds
     *
     * @param targetClass 目标类
     */
    @PostMapping("/{targetClass}/findByIds")
    public List findByIds(@PathVariable("targetClass") String targetClass, @RequestBody FindByIdsQueryPayload payload) throws ClassNotFoundException {
        Assert.notNull(targetClass,"targetClass 不可为空");
        return simpleTransDiver.findByIds(payload.getIds(), (Class<? extends VO>) Class.forName(targetClass)).stream().map(vo->{
            try {
                return vo2BasicVO(vo);
            } catch (IllegalAccessException e) {
               return null;
            }
        }).collect(Collectors.toList());
    }

    /**
     * vo转basicvo
     * @param vo
     * @return
     * @throws IllegalAccessException
     */
    private BasicVO vo2BasicVO(VO vo) throws IllegalAccessException {
        BasicVO result = new BasicVO();
        result.setId(ConverterUtils.toString(vo.getPkey()));
        List<Field> fields = ReflectUtils.getAllField(vo.getClass());
        for (Field field : fields) {
            field.setAccessible(true);
            result.getObjContentMap().put(field.getName(),field.get(vo));
        }
        return result;
    }

    /**
     * 根据id查询
     *
     * @param targetClass 目标类
     */
    @GetMapping("/{targetClass}/findById/{id}")
    public Object findByIds(@PathVariable("targetClass") String targetClass,@PathVariable("id") String id) throws ClassNotFoundException, IllegalAccessException {
        Assert.notNull(targetClass,"targetClass 不可为空");
        Assert.notNull(targetClass,"id 不可为空");
        return vo2BasicVO(simpleTransDiver.findById(id, (Class<? extends VO>) Class.forName(targetClass)));
    }
}
