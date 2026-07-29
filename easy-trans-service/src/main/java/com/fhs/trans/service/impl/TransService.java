package com.fhs.trans.service.impl;

import com.fhs.trans.manager.ClassInfo;
import com.fhs.trans.manager.ClassManager;
import com.fhs.core.trans.vo.VO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 翻译服务
 * 根据类的需要翻译的type 调用对应的trans服务翻译一个或者多个bean
 *
 * @author wanglei
 */
public class TransService {

    private static Logger logger = LoggerFactory.getLogger(TransService.class);

    /**
     * key type  val是对应type的service
     */
    private static Map<String, ITransTypeService> transTypeServiceMap = new HashMap<String, ITransTypeService>();

    /**
     * 注册一个trans服务
     *
     * @param type             类型
     * @param transTypeService 对应的trans接口实现
     */
    public static void registerTransType(String type, ITransTypeService transTypeService) {
        transTypeServiceMap.put(type, transTypeService);
    }

    /**
     * 翻译一个字段
     *
     * @param obj 需要翻译的对象
     */
    public void transOne(VO obj) {
        if (obj == null) {
            return;
        }
        ClassInfo info = ClassManager.getClassInfoByName(obj.getClass());
        String[] transTypes = info.getTransTypes();
        if (transTypes == null) {
            return;
        }
        List<Field> transFieldList = null;
        for (String type : transTypes) {
            transFieldList = info.getTransField(type);
            if (transFieldList == null || transFieldList.size() == 0) {
                continue;
            }
            transTypeServiceMap.get(type).transOne(obj, transFieldList);
        }
    }

    /**
     * 翻译多个VO
     *
     * @param objList 需要翻译的对象集合
     * @param objList 需要翻译的字段集合
     */
    public void transMore(List<? extends VO> objList) {
        if (objList == null || objList.size() == 0) {
            return;
        }
        Object object = objList.get(0);
        ClassInfo info = ClassManager.getClassInfoByName(object.getClass());
        String[] transTypes = info.getTransTypes();
        if (transTypes == null) {
            return;
        }
        List<Field> transFieldList = null;
        for (String type : transTypes) {
            transFieldList = info.getTransField(type);
            if (transFieldList == null || transFieldList.size() == 0) {
                continue;
            }
            ITransTypeService transTypeService = transTypeServiceMap.get(type);
            if (ObjectUtils.isEmpty(transTypeService)) {
                logger.warn("没有匹配的转换器:" + type);
                continue;
            }
            transTypeService.transMore(objList, transFieldList);
        }
    }

    /**
     * 翻译多个VO
     *
     * @param objList
     */
    public void transBatch(List<? extends VO> objList) {
        transMore(objList);
    }


}
