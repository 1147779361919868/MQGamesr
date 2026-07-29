package com.fhs.trans.controller;

import com.baomidou.mybatisplus.annotation.TableId;
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
        List<? extends Serializable> ids = payload.getIds();
        Class fieldType = getPkeyFieldType(targetClass);
        // 如果字段类型不是String，则转换
        if (fieldType == int.class || fieldType == Integer.class) {
            ids = payload.getIds().stream().filter(id -> {
                return id != null && !id.isEmpty();
            }).map(Integer::valueOf).collect(Collectors.toList());
        } else if (fieldType == long.class || fieldType == Long.class) {
            ids = payload.getIds().stream().filter(id -> {
                return id != null && !id.isEmpty();
            }).map(Long::valueOf).collect(Collectors.toList());
        }
        return simpleTransDiver.findByIds(ids, (Class<? extends VO>) Class.forName(targetClass),payload.getUniqueField()).stream().map(vo -> {
            try {
                return vo2BasicVO(vo);
            } catch (IllegalAccessException e) {
                return null;
            }
        }).collect(Collectors.toList());
    }

    /**
     * 获取主键字段类型
     * @param targetClass po类名
     * @return 主键字段类型
     * @throws ClassNotFoundException 如果类不存在
     */
    private Class getPkeyFieldType(String targetClass) throws ClassNotFoundException {
        Class<? extends VO> clazz = (Class<? extends VO>) Class.forName(targetClass);
        List<Field> fieldList = ReflectUtils.getAnnotationField(clazz, javax.persistence.Id.class);
        if (fieldList.size() == 0) {
            fieldList = ReflectUtils.getAnnotationField(clazz, TableId.class);
            if (fieldList.size() == 0) {
                throw new RuntimeException("找不到" + clazz + "的id注解");
            }
        }
        return fieldList.get(0).getType();
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
    public Object findById(@PathVariable("targetClass") String targetClass, @PathVariable("id") String id, @RequestParam("uniqueField")String uniqueField) throws ClassNotFoundException, IllegalAccessException {
        Assert.notNull(targetClass, "targetClass 不可为空");
        Assert.notNull(targetClass, "id 不可为空");
        Serializable sid = id;
        Class fieldType = getPkeyFieldType(targetClass);
        // 如果字段类型不是String，则转换
        if (fieldType == int.class || fieldType == Integer.class) {
            sid = Integer.valueOf(id);
        } else if (fieldType == long.class || fieldType == Long.class) {
            sid = Long.valueOf(id);
        }
        VO vo = simpleTransDiver.findById(sid, (Class<? extends VO>) Class.forName(targetClass),uniqueField);
        if (vo == null) {
            return null;
        }
        return vo2BasicVO(vo);
    }

}
