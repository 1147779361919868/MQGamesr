package com.fhs.trans.service.impl;

import com.fhs.common.utils.CheckUtils;
import com.fhs.common.utils.ConverterUtils;
import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.constant.TransType;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 简单翻译
 */
public class EnumTransService implements ITransTypeService, InitializingBean {

    public static final Logger LOGGER = LoggerFactory.getLogger(EnumTransService.class);


    @Override
    public void transOne(VO obj, List<Field> toTransList) {
        Trans tempTrans = null;
        for (Field tempField : toTransList) {
            try {
                tempTrans = tempField.getAnnotation(Trans.class);
                tempField.setAccessible(true);
                Object filedValue = tempField.get(obj);
                if (filedValue == null) {
                    continue;
                }
                if (!filedValue.getClass().isEnum()) {
                    LOGGER.error("TransType.ENUM 必须加到枚举字段上(暂时不支持枚举数组)，当前字段:" + tempField.getName());
                    continue;
                }
                Field titleField = ReflectUtils.getDeclaredField(filedValue.getClass(), tempTrans.key());
                titleField.setAccessible(true);
                String transResult = ConverterUtils.toString(titleField.get(filedValue));
                setRef(tempTrans, obj, transResult);
                if (obj.getTransMap() != null) {
                    obj.getTransMap().put(tempField.getName() + "Name", transResult);
                }
            } catch (IllegalAccessException e) {
                LOGGER.error("IllegalAccessException", e);
            }catch (Exception e){
                LOGGER.error("trans enum error", e);
            }
        }
    }

    @Override
    public void transMore(List<? extends VO> objList, List<Field> toTransList) {
        objList.forEach(obj -> {
            this.transOne(obj, toTransList);
        });
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        TransService.registerTransType(TransType.ENUM, this);
    }


}
