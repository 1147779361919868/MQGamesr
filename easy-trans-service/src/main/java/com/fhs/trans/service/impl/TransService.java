package com.fhs.trans.service.impl;

import com.fhs.core.trans.anno.Trans;
import com.fhs.trans.manager.ClassInfo;
import com.fhs.trans.manager.ClassManager;
import com.fhs.core.trans.vo.VO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.util.*;

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
    private static Map<String, ITransTypeService> transTypeServiceMap = new LinkedHashMap<>();

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
        trans(null,obj);
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
        trans(objList,null);
    }

    /**
     * 如果objList 不为null就走 transMore 否则就走transOne
     * @param objList 需要被翻译的集合
     * @param obj 需要被翻译的单个对象
     */
    private void trans(List<? extends VO> objList,VO obj){
        ClassInfo info = ClassManager.getClassInfoByName(obj!=null ? obj.getClass() : objList.get(0).getClass());
        if (info.getTransTypes() == null) {
            return;
        }
        Set<String> transTypes = new HashSet<>(Arrays.asList(info.getTransTypes()));
        List<Field> transFieldList = null;
        for (String type : transTypeServiceMap.keySet()) {
            if (!transTypes.contains(type)) {
                continue;
            }
            transFieldList = info.getTransField(type);
            if (transFieldList == null || transFieldList.size() == 0) {
                continue;
            }
            //根据sort排序 小的排到前面
            transFieldList.sort(new Comparator<Field>() {
                @Override
                public int compare(Field o1, Field o2) {
                    return o1.getAnnotation(Trans.class).sort() - o2.getAnnotation(Trans.class).sort();
                }
            });
            ITransTypeService transTypeService = transTypeServiceMap.get(type);
            if (ObjectUtils.isEmpty(transTypeService)) {
                logger.warn("没有匹配的转换器:" + type);
                continue;
            }
            if(objList!=null){
                transTypeService.transMore(objList, transFieldList);
            }
            else{
                transTypeService.transOne(obj, transFieldList);
            }
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
