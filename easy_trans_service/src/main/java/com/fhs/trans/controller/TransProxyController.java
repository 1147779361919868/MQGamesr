package com.fhs.trans.controller;

import com.fhs.common.utils.ConverterUtils;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.SimpleTransService;
import com.fhs.trans.vo.BasicVO;
import com.fhs.trans.vo.FindByIdsQueryPayload;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.persistence.Id;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
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
        Assert.notNull(targetClass, "targetClass 不可为空");
        Class<? extends VO> clazz = (Class<? extends VO>) Class.forName(targetClass);
        List<? extends Serializable> ids = payload.getIds();
        List<Field> pkeyFileds = ReflectUtils.getAnnotationField(clazz, Id.class);
        if (pkeyFileds.isEmpty()) {
            throw new IllegalArgumentException("没有找到主键字段");
        }
        Field pkeyField = pkeyFileds.get(0);
        Class fieldType = pkeyField.getType();
        // 如果字段类型不是String，则转换
        if (fieldType == int.class || fieldType == Integer.class) {
            ids = payload.getIds().stream().filter(id->{
                return id != null && !id.isEmpty();
            }).map(Integer::valueOf).collect(Collectors.toList());
        }else if (fieldType == long.class || fieldType == Long.class) {
            ids = payload.getIds().stream().filter(id->{
                return id != null && !id.isEmpty();
            }).map(Long::valueOf).collect(Collectors.toList());
        }
        return simpleTransDiver.findByIds(ids, (Class<? extends VO>) Class.forName(targetClass)).stream().map(vo -> {
            try {
                return vo2BasicVO(vo);
            } catch (IllegalAccessException e) {
                return null;
            }
        }).collect(Collectors.toList());
    }

    /**
     * vo转basicvo
     *
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
            result.getObjContentMap().put(field.getName(), field.get(vo));
        }
        return result;
    }

    /**
     * 根据id查询
     *
     * @param targetClass 目标类
     */
    @GetMapping("/{targetClass}/findById/{id}")
    public Object findById(@PathVariable("targetClass") String targetClass, @PathVariable("id") String id) throws ClassNotFoundException, IllegalAccessException {
        Assert.notNull(targetClass, "targetClass 不可为空");
        Assert.notNull(targetClass, "id 不可为空");
        Serializable sid = id;
        Class<? extends VO> clazz = (Class<? extends VO>) Class.forName(targetClass);
        List<Field> pkeyFileds = ReflectUtils.getAnnotationField(clazz, Id.class);
        if (pkeyFileds.isEmpty()) {
            throw new IllegalArgumentException("没有找到主键字段");
        }
        Field pkeyField = pkeyFileds.get(0);
        Class fieldType = pkeyField.getType();
        // 如果字段类型不是String，则转换
        if (fieldType == int.class || fieldType == Integer.class) {
            sid = Integer.valueOf(id);
        }else if (fieldType == long.class || fieldType == Long.class) {
            sid = Long.valueOf(id);
        }
        VO vo = simpleTransDiver.findById(sid, (Class<? extends VO>) Class.forName(targetClass));
        if (vo == null) {
            return null;
        }
        return vo2BasicVO(vo);
    }

}
