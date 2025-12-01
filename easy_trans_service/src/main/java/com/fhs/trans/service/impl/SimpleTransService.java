package com.fhs.trans.service.impl;

import com.fhs.common.utils.CheckUtils;
import com.fhs.common.utils.ConverterUtils;
import com.fhs.core.trans.anno.AutoTrans;
import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.constant.TransType;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.TransPojo;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.ds.DataSourceSetter;
import com.fhs.trans.listener.TransMessageListener;
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
public class SimpleTransService implements ITransTypeService, InitializingBean {

    public static final Logger LOGGER = LoggerFactory.getLogger(SimpleTransService.class);

    /**
     * 如果直接去表里查询，放到这个cache中
     */
    private ThreadLocal<Map<String, Map<String, String>>> threadLocalCache = new ThreadLocal<>();

    protected SimpleTransDiver transDiver;

    /**
     * 设置数据源
     */
    protected DataSourceSetter dataSourceSetter;

    /**
     * 注册翻译驱动
     *
     * @param transDiver
     */
    public void regsiterTransDiver(SimpleTransDiver transDiver) {
        this.transDiver = transDiver;
    }

    @Override
    public void transOne(VO obj, List<Field> toTransList) {
        Trans tempTrans = null;
        for (Field tempField : toTransList) {
            tempTrans = tempField.getAnnotation(Trans.class);
            String alias = tempTrans.alias();
            String pkey = ConverterUtils.toString(ReflectUtils.getValue(obj, tempField.getName()));
            if (StringUtils.isEmpty(pkey)) {
                continue;
            }
            Map<String, String> transCache = null;
            // 主键可能是数组
            pkey = pkey.replace("[", "").replace("]", "");
            if (pkey.contains(",")) {
                String[] pkeys = pkey.split(",");
                transCache = new LinkedHashMap<>();
                Map<String, String> tempTransCache = null;
                for (String tempPkey : pkeys) {
                    tempTransCache = getTempTransCacheMap(tempTrans, tempPkey);
                    if (tempTransCache == null) {
                        LOGGER.error(this.getClass().getName() + "缓存未命中:" + tempTrans.target().getName() + "_" + tempPkey);
                        continue;
                    }
                    // 比如学生表  可能有name和age 2个字段
                    for (String key : tempTransCache.keySet()) {
                        transCache.put(key, transCache.containsKey(key) ? transCache.get(key) + "," + tempTransCache.get(key) : tempTransCache.get(key));
                    }
                }
            } else {
                transCache = getTempTransCacheMap(tempTrans, ReflectUtils.getValue(obj, tempField.getName()));
                if (transCache == null) {
                    LOGGER.error(this.getClass().getName() + "缓存未命中:" + tempTrans.target().getName() + "_" + pkey);
                    continue;
                }
            }
            setRef(tempTrans, obj, transCache);
            Map<String, String> transMap = obj.getTransMap();
            if (transMap == null) {
                continue;
            }
            if (!CheckUtils.isNullOrEmpty(alias)) {
                Map<String, String> tempMap = new HashMap<>();
                Set<String> keys = transCache.keySet();
                for (String key : keys) {
                    tempMap.put(alias + key.substring(0, 1).toUpperCase() + key.substring(1), transCache.get(key));
                }
                transCache = tempMap;
            }
            Set<String> keys = transCache.keySet();
            for (String key : keys) {
                if (CheckUtils.isNullOrEmpty(transMap.get(key))) {
                    transMap.put(key, transCache.get(key));
                }
            }
        }
    }

    @Override
    public void transMore(List<? extends VO> objList, List<Field> toTransList) {
        threadLocalCache.set(new HashMap<>());
        // 根据namespace区分
        Map<String,List<Field>> namespaceFieldsGroupMap = new HashMap<>();
        for (Field tempField : toTransList) {
            tempField.setAccessible(true);
            Trans tempTrans = tempField.getAnnotation(Trans.class);
            String targetClassName = getTargetClassName(tempTrans);
            List<Field> fields = namespaceFieldsGroupMap.containsKey(targetClassName) ? namespaceFieldsGroupMap.get(tempTrans.target()) : new ArrayList<>();
            fields.add(tempField);
            namespaceFieldsGroupMap.put(targetClassName,fields);
        }
        // 合并相同的一次in过来
        for (String target : namespaceFieldsGroupMap.keySet()) {
            final List<Field> fields = namespaceFieldsGroupMap.get(target);
            Trans tempTrans = fields.get(0).getAnnotation(Trans.class);
            Set<Object> ids = new HashSet<>();
            objList.forEach(obj -> {
                for (Field field : fields) {
                    try {
                        Object tempId = field.get(obj);
                        if (CheckUtils.isNotEmpty(tempId)) {
                            String pkey = ConverterUtils.toString(tempId).replace("[", "").replace("]", "");
                            if (pkey.contains(",")) {
                                String[] pkeys = pkey.split(",");
                                for (String id : pkeys) {
                                    ids.add(id);
                                }
                            } else {
                                ids.add(tempId);
                            }
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }

            });
            if (!ids.isEmpty()) {
                List<? extends VO> dbDatas = findByIds(new ArrayList<Object>(ids), tempTrans);
                for (VO vo : dbDatas) {
                    threadLocalCache.get().put(getTargetClassName(tempTrans) + "_" + vo.getPkey(),
                            createTempTransCacheMap(vo, tempTrans));
                }
            }
        }
        objList.forEach(obj -> {
            this.transOne(obj, toTransList);
        });
        threadLocalCache.set(null);
    }

    /**
     * 根据id 集合 获取数据
     *
     * @param ids
     * @param tempTrans
     * @return
     */
    public List<? extends VO> findByIds(List ids, Trans tempTrans) {
        return findByIds(()->{
            return transDiver.findByIds(ids, tempTrans.target());
        },tempTrans.dataSource());

    }

    /**
     * 根据id查询单个
     *
     * @param id
     * @param tempTrans
     * @return
     */
    public VO findById(Object id, Trans tempTrans) {
       return findById(()->{
            return transDiver.findById((Serializable)id, tempTrans.target());
        },tempTrans.dataSource());
    }

    /**
     * 获取用于翻译的缓存
     *
     * @param tempTrans tempTrans
     * @param pkey      主键
     * @return 缓存
     */
    private Map<String, String> getTempTransCacheMap(Trans tempTrans, Object pkey) {
        if (this.threadLocalCache.get() == null) {
            if (CheckUtils.isNullOrEmpty(pkey)) {
                return new HashMap<>();
            }
            VO vo = this.findById(pkey, tempTrans);
            return createTempTransCacheMap(vo, tempTrans);
        }
        // 兼容RPC Trans
        return this.threadLocalCache.get().get(getTargetClassName(tempTrans) + "_" + pkey);
    }

    /**
     * 因为要兼容RPC Trans 所以这里这么写
     * @param tempTrans tempTrans
     * @return
     */
    protected String getTargetClassName(Trans tempTrans){
       return (tempTrans.target() == TransPojo.class
                ?  tempTrans.targetClassName(): tempTrans.target().getName());
    }

    /**
     * 创建一个临时缓存map
     *
     * @param po    po
     * @param trans 配置
     * @return
     */
    protected Map<String, String> createTempTransCacheMap(Object po, Trans trans) {
        String fielVal = null;
        Map<String, String> tempCacheTransMap = new LinkedHashMap<>();
        if (po == null) {
            return tempCacheTransMap;
        }
        for (String field : trans.fields()) {
            fielVal = ConverterUtils.toString(ReflectUtils.getValue(po, field));
            tempCacheTransMap.put(field, fielVal);
        }
        return tempCacheTransMap;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        TransService.registerTransType(TransType.SIMPLE, this);
    }

    /**
     * 简单翻译数据驱动
     */
    public static interface SimpleTransDiver {
        /**
         * 根据ids获取集合
         *
         * @param ids ids
         * @return
         */
        List<? extends VO> findByIds(List<? extends Serializable> ids, Class<? extends VO> targetClass);

        /**
         * 根据id查询对象
         *
         * @param id id
         * @return
         */
        VO findById(Serializable id, Class<? extends VO> targetClass);
    }
}
