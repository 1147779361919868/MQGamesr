package com.fhs.trans.service.impl;

import com.fhs.common.constant.Constant;
import com.fhs.common.utils.StringUtil;
import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.constant.TransType;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字典翻译服务
 *
 * @author jackwang
 * @date 2020-05-18 14:41:20
 */
public class DictionaryTransService implements ITransTypeService, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryTransService.class);

    /**
     * 用来放字典缓存的map
     */
    private Map<String, String> dictionaryTransMap = new ConcurrentHashMap<>();

    /**
     * 刷新缓存
     *
     * @param dicTypeCode 字典类型编码
     * @param dicMap      字典map
     */
    public void refreshCache(String dicTypeCode, Map<String, String> dicMap) {
        dicMap.keySet().forEach(key -> {
            dictionaryTransMap.put(dicTypeCode + "_" + key, dicMap.get(key));
        });

    }


    @Override
    public void transOne(VO obj, List<Field> toTransList) {
        Trans tempTrans = null;

        for (Field tempField : toTransList) {
            tempField.setAccessible(true);
            tempTrans = tempField.getAnnotation(Trans.class);
            String dicCodes = StringUtil.toString(ReflectUtils.getValue(obj, tempField.getName()));
            String[] dicCodeArray = dicCodes.split(",");
            String key = tempTrans.key().contains("KEY_") ? StringUtil.toString(ReflectUtils.getValue(obj, tempTrans.key().replace("KEY_", ""))) : tempTrans.key();
            //sex_0/1  男 女
            List<String> dicCodeList = new ArrayList<>();
            for (String dicCode : dicCodeArray) {
                if (!StringUtil.isEmpty(dicCode)) {
                    dicCodeList.add(dictionaryTransMap.get(key + "_" + dicCode));
                }
            }
            String transResult = dicCodeList.size() > Constant.ZERO ? StringUtil.getStrForIn(dicCodeList, false) : "";
            if (obj.getTransMap() != null) {
                obj.getTransMap().put(tempField.getName() + "Name", transResult);
            }
            setRef(tempTrans, obj, transResult);
        }
    }

    @Override
    public void transMore(List<? extends VO> objList, List<Field> toTransList) {
        for (VO obj : objList) {
            transOne(obj, toTransList);
        }
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        //注册自己为一个服务
        TransService.registerTransType(TransType.DICTIONARY, this);
    }


}
