package com.fhs.trans.service.impl;


import com.fhs.common.utils.CheckUtils;
import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * 翻译接口,将此接口实现类注册到transservice即可用
 *
 * @author wanglei
 */
public interface ITransTypeService {
    /**
     * 翻译一个字段
     *
     * @param obj         需要翻译的对象
     * @param toTransList 需要翻译的字段
     */
    void transOne(VO obj, List<Field> toTransList);

    /**
     * 翻译多个 字段
     *
     * @param objList     需要翻译的对象集合
     * @param toTransList 需要翻译的字段集合
     */
    void transMore(List<? extends VO> objList, List<Field> toTransList);

    /**
     * 设置ref
     *
     * @param trans 注解对象
     * @param vo    等待被翻译的数据
     * @param val   翻译的值
     */
    default void setRef(Trans trans, VO vo, String val) {
        if (CheckUtils.isNotEmpty(trans.ref())) {
            ReflectUtils.setValue(vo, trans.ref(), val);
        }
    }

    default void setRef(Trans trans, VO vo, Map<String,String> valMap) {
        if (CheckUtils.isNotEmpty(trans.ref())) {
            String ref = trans.ref();
            String[] refSetting = ref.split("#");
            if (refSetting.length == 1) {
                if(valMap.size()>0){
                   String key = valMap.keySet().iterator().next();
                   ReflectUtils.setValue(vo, refSetting[0], valMap.get(key));
                }
            } else if (refSetting.length == 2) {
                ReflectUtils.setValue(vo, refSetting[0], valMap.get(refSetting[1]));
            }
        }
    }

}
