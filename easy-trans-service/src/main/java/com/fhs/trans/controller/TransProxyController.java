package com.fhs.trans.controller;

import com.fhs.common.utils.ConverterUtils;
import com.fhs.common.utils.StringUtil;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.SimpleTransService;
import com.fhs.trans.vo.BasicVO;
import com.fhs.trans.vo.FindByIdsQueryPayload;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        Set<String> targetFields = null;
        if(payload.getTargetFields()!=null && payload.getTargetFields().length!=0){
            targetFields = new HashSet<>(Arrays.asList(payload.getTargetFields()));
        }
        return simpleTransDiver.findByIds(ids, (Class<? extends VO>) Class.forName(targetClass),payload.getUniqueField(),targetFields).stream().map(vo -> {
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
        return ReflectUtils.getIdField(Class.forName(targetClass),true).getType();
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
    public Object findById(@PathVariable("targetClass") String targetClass, @PathVariable("id") String id, @RequestParam("uniqueField")String uniqueField, @RequestParam("targetFields")String targetFields) throws ClassNotFoundException, IllegalAccessException {
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
        Set<String> targetFieldSet = null;
        if(!StringUtil.isEmpty(targetFields) && !"null".equals(targetFields)){
            targetFieldSet = new HashSet<>(Arrays.asList(targetFields.split(",")));
        }
        VO vo = simpleTransDiver.findById(sid, (Class<? extends VO>) Class.forName(targetClass),uniqueField,targetFieldSet);
        if (vo == null) {
            return null;
        }
        return vo2BasicVO(vo);
    }

}
