package com.fhs.trans.service.impl;

import com.fhs.common.utils.CheckUtils;
import com.fhs.common.utils.ConverterUtils;
import com.fhs.common.utils.JsonUtils;
import com.fhs.common.utils.StringUtil;
import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.anno.TransDefaultSett;
import com.fhs.core.trans.constant.TransType;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.ds.DataSourceSetter;
import com.fhs.trans.listener.TransMessageListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
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
    private ThreadLocal<Map<String, Map<String, Object>>> threadLocalCache = new ThreadLocal<>();

    protected SimpleTransDiver transDiver;

    /**
     * 设置数据源
     */
    protected DataSourceSetter dataSourceSetter;

    /**
     * 缓存配置
     */
    protected Map<String, TransCacheSett> transCacheSettMap = new HashMap<>();

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
            tempTrans = new SimpleTrans(tempField.getAnnotation(Trans.class));
            String alias = tempTrans.alias();
            String pkey = ConverterUtils.toString(ReflectUtils.getValue(obj, tempField.getName()));
            if (StringUtils.isEmpty(pkey)) {
                continue;
            }
            Map<String, String> transCache = null;
            // 主键可能是数组
            pkey = pkey.replace("[", "").replace("]", "");
            Map<String, Object> tempTransCache = null;
            boolean isMany = false;
            Object targetObject = null;
            if (pkey.contains(",")) {
                isMany = true;
                String[] pkeys = pkey.split(",");
                transCache = new LinkedHashMap<>();

                for (String tempPkey : pkeys) {
                    tempTransCache = getTempTransCacheMap(tempTrans, tempPkey);
                    if (tempTransCache == null || tempTransCache.isEmpty()) {
                        LOGGER.warn(this.getClass().getName() + "翻译未命中数据:" + tempTrans.target().getName() + "_" + tempPkey);
                        continue;
                    }
                    tempTransCache.remove("targetObject");
                    // 比如学生表  可能有name和age 2个字段
                    for (String key : tempTransCache.keySet()) {
                        transCache.put(key, transCache.containsKey(key) ? transCache.get(key) + "," + tempTransCache.get(key)
                                : StringUtil.toString(tempTransCache.get(key)));
                    }
                }
            } else {
                transCache = new LinkedHashMap<>(1);
                tempTransCache = getTempTransCacheMap(tempTrans, ReflectUtils.getValue(obj, tempField.getName()));
                if (tempTransCache == null || tempTransCache.isEmpty()) {
                    LOGGER.warn(this.getClass().getName() + "翻译未命中数据:" + tempTrans.target().getName() + "_" + ReflectUtils.getValue(obj, tempField.getName()));
                    continue;
                }
                Map<String, String> finalTransCache = transCache;
                for (Map.Entry<String, Object> stringObjectEntry : tempTransCache.entrySet()) {
                    if (!"targetObject".equals(stringObjectEntry.getKey())) {
                        finalTransCache.put(stringObjectEntry.getKey(), StringUtil.toString(stringObjectEntry.getValue()));
                    }else{
                        targetObject = stringObjectEntry.getValue();
                    }
                }
            }
            if (tempTransCache != null) {
                setRef(tempTrans, obj, transCache, (VO) targetObject);
            }

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
        // 根据namespace区分本vo有哪些字段
        Map<String, List<Field>> namespaceFieldsGroupMap = new HashMap<>();
        // 根据namespace区分对方po有哪些字段需要从db查询出来
        Map<String, Set<String>> namespaceTargetFieldsGroupMap = new HashMap<>();
        for (Field tempField : toTransList) {
            tempField.setAccessible(true);
            Trans tempTrans = tempField.getAnnotation(Trans.class);
            String targetClassName = getTargetClassName(tempTrans);
            List<Field> fields = namespaceFieldsGroupMap.containsKey(targetClassName) ? namespaceFieldsGroupMap.get(targetClassName) : new ArrayList<>();
            Set<String> targetFields = namespaceTargetFieldsGroupMap.containsKey(targetClassName) ? namespaceTargetFieldsGroupMap.get(targetClassName) : new HashSet<>();
            targetFields.addAll(Arrays.asList(tempTrans.fields()));
            fields.add(tempField);
            namespaceFieldsGroupMap.put(targetClassName, fields);
            namespaceTargetFieldsGroupMap.put(targetClassName, targetFields);
        }
        // 合并相同的一次in过来
        for (String target : namespaceFieldsGroupMap.keySet()) {
            final List<Field> fields = namespaceFieldsGroupMap.get(target);
            Trans tempTrans = new SimpleTrans(fields.get(0).getAnnotation(Trans.class));
            final Set<Object> ids = new HashSet<>();
            Set<String> targetFields = namespaceTargetFieldsGroupMap.get(target);
            targetFields.addAll(this.getTargetDefaultFields(tempTrans));
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
                if (transCacheSettMap.containsKey(getTargetClassName(tempTrans))) {
                    //如果使用缓存了那么就不指定字段了，避免部分vo翻译不全
                    targetFields = null;
                    Set<Object> newIds = initLocalFromGlobalCache(threadLocalCache, ids, getTargetClassName(tempTrans), this.getClass() == SimpleTransService.class ?
                            TransType.SIMPLE : TransType.RPC);
                    ids.clear();
                    ids.addAll(newIds);
                }
                if (!ids.isEmpty()) {
                    List<? extends VO> dbDatas = findByIds(new ArrayList<Object>(ids), tempTrans, targetFields);
                    for (VO vo : dbDatas) {
                        threadLocalCache.get().put(getTargetClassName(tempTrans) + "_" + getUniqueKey(vo, tempTrans),
                                createTempTransCacheMap(vo, tempTrans,targetFields));
                    }
                }
            }
        }
        objList.forEach(obj -> {
            this.transOne(obj, toTransList);
        });
        threadLocalCache.set(null);
    }

    /**
     * 获取默认字段
     *
     * @param tempTrans 翻译配置
     * @return
     */
    private List<String> getTargetDefaultFields(Trans tempTrans) {
        if (tempTrans.target() != com.fhs.core.trans.vo.TransPojo.class && !transCacheSettMap.containsKey(tempTrans.target())
                && tempTrans.target().isAnnotationPresent(TransDefaultSett.class)) {
            TransDefaultSett transDefaultSett = tempTrans.target().getAnnotation(TransDefaultSett.class);
            return Arrays.asList(transDefaultSett.defaultFields());
        }
        return new ArrayList<>();
    }

    /**
     * 获取唯一键
     *
     * @param vo        vo
     * @param tempTrans 翻译注解
     * @return
     */
    public Object getUniqueKey(VO vo, Trans tempTrans) {
        if (StringUtils.isEmpty(tempTrans.uniqueField())) {
            return vo.getPkey();
        }
        return ReflectUtils.getValue(vo, tempTrans.uniqueField());
    }

    /**
     * 根据id 集合 获取数据
     *
     * @param ids          主键
     * @param tempTrans    翻译配置
     * @param targetFields 需要查询的字段
     * @return
     */
    public List<? extends VO> findByIds(List ids, Trans tempTrans, Set<String> targetFields) {
        return findByIds(() -> {
            return transDiver.findByIds(ids, tempTrans.target(), tempTrans.uniqueField());
        }, tempTrans.dataSource());
    }

    /**
     * 根据id查询单个
     *
     * @param id
     * @param tempTrans
     * @return
     */
    public VO findById(Object id, Trans tempTrans) {
        return findById(() -> {
            HashSet<String> fields = new HashSet<>(Arrays.asList(tempTrans.fields()));
            if(transCacheSettMap.containsKey(getTargetClassName(tempTrans))){
                fields = null;
            }
            return transDiver.findById((Serializable) id, tempTrans.target(), tempTrans.uniqueField(),fields);
        }, tempTrans.dataSource());
    }

    /**
     * 获取用于翻译的缓存
     *
     * @param tempTrans tempTrans
     * @param pkey      主键
     * @return 缓存
     */
    private Map<String, Object> getTempTransCacheMap(Trans tempTrans, Object pkey) {
        String className = getTargetClassName(tempTrans);
        String transType = this.getClass() == SimpleTransService.class ?
                TransType.SIMPLE : TransType.RPC;
        Map<String, Object>  voCacheMap = getFromGlobalCache(pkey, className, transType);
        // 如果有缓存，则使用缓存不查DB
        if (transCacheSettMap.containsKey(className) && voCacheMap != null) {
            Map<String, Object> resultMap = new LinkedHashMap<>();
            for (String field : tempTrans.fields()) {
                resultMap.put(field,voCacheMap.get(field));
            }
            return resultMap;
         //没有缓存尝试 threadLocalCache 又是空的话 直接去查询
        } else if (this.threadLocalCache.get() == null) {
            if (CheckUtils.isNullOrEmpty(pkey)) {
                return new HashMap<>();
            }
            VO vo = this.findById(pkey, tempTrans);
            return createTempTransCacheMap(vo, tempTrans,null);
        }else{
            // 有缓存 但是不一定匹配到
            voCacheMap = this.threadLocalCache.get().get(getTargetClassName(tempTrans) + "_" + pkey);
            if(voCacheMap == null){
                return null;
            }
            Map<String, Object> resultMap = new LinkedHashMap<>();
            for (String field : tempTrans.fields()) {
                resultMap.put(field,voCacheMap.get(field));
            }
            return resultMap;
        }
    }

    /**
     * 因为要兼容RPC Trans 所以这里这么写
     *
     * @param tempTrans tempTrans
     * @return
     */
    protected String getTargetClassName(Trans tempTrans) {
        if (tempTrans.target() != com.fhs.core.trans.vo.TransPojo.class && !transCacheSettMap.containsKey(tempTrans.target())
                && tempTrans.target().isAnnotationPresent(TransDefaultSett.class)) {
            TransDefaultSett transDefaultSett = tempTrans.target().getAnnotation(TransDefaultSett.class);
            if (transDefaultSett.isUseCache()) {
                TransCacheSett cacheSett = new TransCacheSett();
                cacheSett.setMaxCache(transDefaultSett.maxCache());
                cacheSett.setCacheSeconds(transDefaultSett.cacheSeconds());
                cacheSett.setAccess(transDefaultSett.isAccess());
                transCacheSettMap.put(tempTrans.target().getName(), cacheSett);
            }
        }
        return (tempTrans.target() == com.fhs.core.trans.vo.TransPojo.class
                ? tempTrans.targetClassName() : tempTrans.target().getName());
    }

    /**
     * 创建一个临时缓存map
     *
     * @param po    po
     * @param trans 配置
     * @return
     */
    protected Map<String, Object> createTempTransCacheMap(VO po, Trans trans,Set<String> targetFields) {
        String fielVal = null;
        Map<String, Object> tempCacheTransMap = new LinkedHashMap<>();
        if (po == null) {
            return tempCacheTransMap;
        }
        List<String> tempFields =  targetFields!=null ? new ArrayList<>(targetFields) : Arrays.asList(trans.fields());
        for (String field : tempFields) {
            fielVal = ConverterUtils.toString(ReflectUtils.getValue(po, field));
            tempCacheTransMap.put(field, fielVal);
        }
        tempCacheTransMap.put("targetObject", po);
        String className = getTargetClassName(trans);
        if (transCacheSettMap.get(className) != null) {
            TransCacheSett cacheSett = transCacheSettMap.get(className);
            Map<String, Object> voCacheMap = new LinkedHashMap<>();
            //缓存对象把所有字段都放进去，因为不知道客户会要啥
            List<Field> fields = ReflectUtils.getAllField(po.getClass());
            for (Field field : fields) {
                voCacheMap.put(field.getName(),ReflectUtils.getValue(po,field.getName()));
            }
            voCacheMap.put("targetObject",po);
            put2GlobalCache(voCacheMap, cacheSett.isAccess(), cacheSett.getCacheSeconds(), cacheSett.getMaxCache(), po.getPkey(),
                    className, this.getClass() == SimpleTransService.class ?
                            TransType.SIMPLE : TransType.RPC);
        }
        return tempCacheTransMap;
    }

    /**
     * 清理本地缓存
     *
     * @param messageMap
     */
    public void onMessage(Map<String, Object> messageMap) {
        String messageType = ConverterUtils.toString(messageMap.get("messageType"));
        switch (messageType) {
            case "clear":
                clearGlobalCache(ConverterUtils.toString(messageMap.get("pkey")),
                        ConverterUtils.toString(messageMap.get("target")),
                        ConverterUtils.toString(messageMap.get("transType")));
                break;
        }


    }


    @Override
    public void afterPropertiesSet() throws Exception {
        TransService.registerTransType(TransType.SIMPLE, this);
        //注册刷新缓存服务
        TransMessageListener.regTransRefresher(TransType.SIMPLE, this::onMessage);
    }

    /**
     * 配置缓存
     *
     * @param type
     * @param cacheSett
     */
    public void setTransCache(Object type, TransCacheSett cacheSett) {
        Class typeClass = (Class) type;

        this.transCacheSettMap.put(typeClass.getName(), cacheSett);
    }

    /**
     * 简单翻译数据驱动
     */
    public static interface SimpleTransDiver {
        /**
         * 根据ids获取集合
         *
         * @param ids         ids
         * @param targetClass 目标类类名
         * @param uniqueField 唯一键字段
         * @return
         */
        List<? extends VO> findByIds(List<? extends Serializable> ids, Class<? extends VO> targetClass, String uniqueField);

        /**
         * 根据ids获取集合
         *
         * @param ids          ids
         * @param targetClass  目标类类名
         * @param uniqueField  唯一键字段
         * @param targetFields 目标字段
         * @return
         */
        default List<? extends VO> findByIds(List<? extends Serializable> ids, Class<? extends VO> targetClass, String uniqueField, Set<String> targetFields) {
            return findByIds(ids, targetClass, uniqueField);
        }

        /**
         * 根据id查询对象
         *
         * @param id          id
         * @param targetClass 目标类类名
         * @param uniqueField 唯一键字段
         * @return
         */
        VO findById(Serializable id, Class<? extends VO> targetClass, String uniqueField);

        /**
         * 根据id查询对象
         *
         * @param id           id
         * @param targetClass  目标类类名
         * @param uniqueField  唯一键字段
         * @param targetFields 目标表的字段
         * @return
         */
        default VO findById(Serializable id, Class<? extends VO> targetClass, String uniqueField, Set<String> targetFields) {
            return findById(id, targetClass, uniqueField);
        }
    }


    /**
     * 翻译缓存配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransCacheSett {

        /**
         * true 按照访问时间计算过期时间 false按照插入时间计算过期时间
         */
        private boolean isAccess = false;
        /**
         * 默认过期时间秒
         */
        private long cacheSeconds = 5;
        /**
         * 最大缓存数量
         */
        private int maxCache = 1000;
    }

}

class SimpleTrans implements Trans {

    private Trans anno;

    /**
     * 翻译字段
     */
    private String[] fields = new String[]{};

    /**
     * 别名
     */
    private String alias = "";

    /**
     * 唯一键
     */
    private String uniqueField = "";

    private String dataSource = "";


    public SimpleTrans(Trans anno) {
        this.anno = anno;
        Class<? extends com.fhs.core.trans.vo.TransPojo> clazz = (Class<? extends com.fhs.core.trans.vo.TransPojo>) anno.target();
        if (clazz != null && clazz.isAnnotationPresent(TransDefaultSett.class)) {
            TransDefaultSett transDefaultSett = clazz.getAnnotation(TransDefaultSett.class);
            this.fields = transDefaultSett.defaultFields();
            this.alias = transDefaultSett.defaultAlias();
            this.uniqueField = transDefaultSett.uniqueField();
            this.dataSource = transDefaultSett.dataSource();
        }
    }

    @Override
    public String type() {
        return anno.type();
    }

    @Override
    public String key() {
        return anno.key();
    }

    @Override
    public String ref() {
        return anno.ref();
    }

    @Override
    public String[] refs() {
        return anno.refs();
    }

    @Override
    public Class<? extends VO> target() {
        return anno.target();
    }

    @Override
    public String[] fields() {
        if (anno.fields().length != 0 || fields == null) {
            return anno.fields();
        }
        return (anno.fields().length != 0 || fields == null) ? anno.fields() : fields;
    }

    @Override
    public String alias() {
        return (anno.alias().length() != 0 || "".equals(alias)) ? anno.alias() : alias;
    }

    @Override
    public String serviceName() {
        return anno.serviceName();
    }

    @Override
    public String serviceContextPath() {
        return anno.serviceContextPath();
    }

    @Override
    public String targetClassName() {
        return anno.targetClassName();
    }

    @Override
    public String customeBeanFuncName() {
        return anno.customeBeanFuncName();
    }

    @Override
    public String dataSource() {
        return (anno.dataSource().length() != 0 || "".equals(dataSource)) ? anno.dataSource() : dataSource;
    }

    @Override
    public String uniqueField() {
        return (anno.uniqueField().length() != 0 || "".equals(uniqueField)) ? anno.uniqueField() : uniqueField;
    }

    @Override
    public int sort() {
        return anno.sort();
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return anno.annotationType();
    }
}