package com.fhs.trans.service.impl;


import com.fhs.cache.service.RedisCacheService;
import com.fhs.common.spring.AnnotationTypeFilterBuilder;
import com.fhs.common.spring.SpringClassScanner;
import com.fhs.common.spring.SpringContextUtil;
import com.fhs.common.utils.CheckUtils;
import com.fhs.common.utils.ConverterUtils;
import com.fhs.core.trans.anno.AutoTrans;
import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.constant.TransType;
import com.fhs.trans.listener.TransMessageListener;
import com.fhs.trans.service.AutoTransAble;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本接类使用需要配合Autotrans 注解和autoTransAble的实现类
 *
 * @Description: 自动翻译服务
 * @Author: Wanglei
 * @Date: Created in 10:14 2019/10/15
 */
public class AutoTransService implements ITransTypeService, InitializingBean, ApplicationListener<ApplicationReadyEvent> {

    public static final Logger LOGGER = LoggerFactory.getLogger(AutoTransService.class);


    /**
     * service的包路径
     */
    private String packageNames;

    /**
     * 翻译数据缓存map
     */
    private Map<String, Map<String, Object>> localTransCacheMap = new HashMap<>();


    /**
     * 缓存 默认时间：半个小时
     */
    private RedisCacheService<Map<String, Object>> redisTransCache;

    /**
     * 基础服务
     */
    private Map<String, AutoTransAble> baseServiceMap = new HashMap<>();

    /**
     * 配置
     */
    private Map<String, AutoTrans> transSettMap = new ConcurrentHashMap<>();

    /**
     * 如果直接去表里查询，放到这个cache中
     */
    private ThreadLocal<Map<String, Map<String, Object>>> threadLocalCache = new ThreadLocal<>();

    /**
     * 翻译字段配置map
     */
    private Map<Field, TransFieldSett> transFieldSettMap = new HashMap<>();

    @Override
    public void transOne(VO obj, List<Field> toTransList) {
        Trans tempTrans = null;
        for (Field tempField : toTransList) {
            TransFieldSett transFieldSett = transFieldSettMap.containsKey(tempField) ? transFieldSettMap.get(tempField) : new TransFieldSett(tempField);
            tempTrans = transFieldSett.getTrans();
            String namespace = transFieldSett.getNamespace();
            String alias = transFieldSett.getAlias();
            if (transSettMap.containsKey(namespace) && CheckUtils.isNullOrEmpty(alias)) {
                alias = transSettMap.get(namespace).defaultAlias();
            }
            String pkey = ConverterUtils.toString(ReflectUtils.getValue(obj, tempField.getName()));
            if (StringUtils.isEmpty(pkey)) {
                continue;
            }
            Map<String, Object> transCache = null;
            // 主键可能是数组
            pkey = pkey.replace("[", "").replace("]", "");
            if (pkey.contains(",")) {
                String[] pkeys = pkey.split(",");
                transCache = new LinkedHashMap<>();
                Map<String, Object> tempTransCache = null;
                for (String tempPkey : pkeys) {
                    tempTransCache = getTempTransCacheMap(namespace, tempPkey);
                    if (tempTransCache == null || tempTransCache.isEmpty()) {
                        LOGGER.warn("auto trans缓存未命中:" + namespace + "_" + tempPkey);
                        continue;
                    }
                    // 比如学生表  可能有name和age 2个字段
                    for (String key : tempTransCache.keySet()) {
                        transCache.put(key, transCache.containsKey(key) ? transCache.get(key) + "," + tempTransCache.get(key) : tempTransCache.get(key));
                    }
                }
            } else {
                //如果不是多个保证原汁原味 的原来的什么类型就是什么类型 而不是string
                transCache = getTempTransCacheMap(namespace, ReflectUtils.getValue(obj, tempField.getName()));
                if (transCache == null || transCache.isEmpty()) {
                    LOGGER.warn("auto trans缓存未命中:" + namespace + "_" + pkey);
                    continue;
                }
            }
            setRef(tempTrans, obj, transCache);
            Map<String, String> transMap = obj.getTransMap();
            if (transMap == null) {
                continue;
            }
            if (!CheckUtils.isNullOrEmpty(alias)) {
                Map<String, Object> tempMap = new HashMap<>();
                Set<String> keys = transCache.keySet();
                for (String key : keys) {
                    tempMap.put(alias + key.substring(0, 1).toUpperCase() + key.substring(1), transCache.get(key));
                }
                transCache = tempMap;
            }

            Set<String> keys = transCache.keySet();

            for (String key : keys) {
                if (CheckUtils.isNullOrEmpty(transMap.get(key))) {
                    transMap.put(key, ConverterUtils.toString(transCache.get(key)));
                }
            }
        }
    }

    @Override
    public void transMore(List<? extends VO> objList, List<Field> toTransList) {
        threadLocalCache.set(new HashMap<>());
        // 根据namespace区分
        Map<String, List<Field>> namespaceFieldsGroupMap = new HashMap<>();
        for (Field tempField : toTransList) {
            tempField.setAccessible(true);
            Trans tempTrans = tempField.getAnnotation(Trans.class);
            String namespace = tempTrans.key();
            // 如果是 good#student  翻译出来应该是 goodStuName goodStuAge  customer#customer  customerName
            if (namespace.contains("#")) {
                namespace = namespace.substring(0, namespace.indexOf("#"));
            }
            if (!this.baseServiceMap.containsKey(namespace)) {
                LOGGER.warn("namesapce对应的service没有标记autotrans:" + namespace);
                continue;
            }
            AutoTrans autoTransSett = this.transSettMap.get(namespace);
            if (autoTransSett.useCache()) {
                continue;
            }
            List<Field> fields = namespaceFieldsGroupMap.containsKey(namespace) ? namespaceFieldsGroupMap.get(namespace) : new ArrayList<>();
            fields.add(tempField);
            namespaceFieldsGroupMap.put(namespace, fields);
        }

        // 由于一些表数据比较多，所以部分数据不是从缓存取的，是从db先放入缓存的，翻译完了释放掉本次缓存的数据
        for (String namespace : namespaceFieldsGroupMap.keySet()) {
            final Set<Object> ids = new HashSet<>();
            final List<Field> fields = namespaceFieldsGroupMap.get(namespace);
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
                if (transSettMap.get(namespace).globalCache()) {
                    Set<Object> newIds = initLocalFromGlobalCache(threadLocalCache, ids, namespace, TransType.AUTO_TRANS);
                    ids.clear();
                    ids.addAll(newIds);
                }
                if(!ids.isEmpty()){
                    AutoTrans autoTransSett = this.transSettMap.get(namespace);
                    if (autoTransSett.useCache()) {
                        continue;
                    }
                    List<? extends VO> dbDatas = findByIds(() -> {
                        return baseServiceMap.get(namespace).findByIds(new ArrayList<>(ids));
                    }, null);

                    for (VO vo : dbDatas) {
                        threadLocalCache.get().put(namespace + "_" + vo.getPkey(), createTempTransCacheMap(vo, autoTransSett));
                    }
                }
            }
        }
        objList.forEach(obj -> {
            this.transOne(obj, toTransList);
        });
        threadLocalCache.set(null);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        TransService.registerTransType(TransType.AUTO_TRANS, this);
        TransMessageListener.regTransRefresher(TransType.AUTO_TRANS, this::refreshCache);
    }


    public void init(ApplicationReadyEvent applicationReadyEvent) {
        //spring容器初始化完成之后，就会自行此方法。
        Set<Class<?>> entitySet = scan(AutoTrans.class, packageNames.split(";"));
        // 遍历所有class，获取所有用@autowareYLM注释的字段
        if (entitySet != null) {
            for (Class<?> entity : entitySet) {
                // 获取该类
                Object baseService = SpringContextUtil.getBeanByName(entity);
                if (!(baseService instanceof AutoTransAble)) {
                    continue;
                }
                AutoTrans autoTransSett = entity.getAnnotation(AutoTrans.class);
                this.baseServiceMap.put(autoTransSett.namespace(), (AutoTransAble) baseService);
                this.transSettMap.put(autoTransSett.namespace(), autoTransSett);
            }
        }
        new Thread(() -> {
            Thread.currentThread().setName("refresh auto trans cache");
            refreshCache(new HashMap<>());
        }).start();

    }

    /**
     * 刷新缓存
     *
     * @param messageMap 消息
     */
    public void refreshCache(Map<String, Object> messageMap) {
        //这里必须能拿到namespace 拿不到,就当作全部刷新
        String namespace = messageMap.get("namespace") != null ?
                messageMap.get("namespace").toString() : null;
        if (namespace == null) {
            Set<String> namespaceSet = this.transSettMap.keySet();
            namespaceSet.forEach(temp -> {
                refreshOneNamespace(temp);
            });
        } else {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.error("刷新缓存错误:", e);
            }
            refreshOneNamespace(namespace);
        }
    }

    /**
     * 刷新一个namespace下的所有的缓存
     *
     * @param namespace namespace
     */
    public void refreshOneNamespace(String namespace) {
        if (!this.transSettMap.containsKey(namespace)) {
            LOGGER.info("本系统无需刷新此缓存namespace:" + namespace);
            return;
        }
        AutoTrans autoTrans = this.transSettMap.get(namespace);
        //不适用缓存的不做缓存
        if (!autoTrans.useCache()) {
            return;
        }
        LOGGER.info("开始刷新auto-trans缓存:" + namespace);
        List<VO> vos = this.baseServiceMap.get(namespace).select();
        if (vos == null || vos.isEmpty()) {
            return;
        }
        Object pkeyVal = null;
        String fielVal = null;
        Map<String, String> tempCacheTransMap = null;
        VO po = null;

        for (int i = 0; i < vos.size(); i++) {
            po = vos.get(i);
            pkeyVal = po.getPkey();
            //如果使用redis则给redis放
            if (autoTrans.useRedis()) {
                this.getRedisTransCache().put(namespace + "_" + pkeyVal, createTempTransCacheMap(po, autoTrans));
            } else {
                //否则给本地缓存放
                localTransCacheMap.put(namespace + "_" + pkeyVal, createTempTransCacheMap(po, autoTrans));
            }
        }
        LOGGER.info("刷新auto-trans缓存完成:" + namespace);
    }

    public RedisCacheService<Map<String, Object>> getRedisTransCache() {
        if (this.redisTransCache == null) {
            throw new IllegalArgumentException("请确定开启了easy-tran.is-enable-redis 为true，springBoot启动已经完成");
        }
        return this.redisTransCache;
    }

    /**
     * 创建一个临时缓存map
     *
     * @param po        po
     * @param autoTrans 配置
     * @return
     */
    private Map<String, Object> createTempTransCacheMap(VO po, AutoTrans autoTrans) {
        Object fielVal = null;
        Map<String, Object> tempCacheTransMap = new LinkedHashMap<>();
        if (po == null) {
            return tempCacheTransMap;
        }
        for (String field : autoTrans.fields()) {
            fielVal = ReflectUtils.getValue(po, field);
            tempCacheTransMap.put(field, fielVal);
        }
        if (autoTrans.globalCache()) {
            put2GlobalCache(tempCacheTransMap, autoTrans.isAccess(), autoTrans.cacheSeconds(), autoTrans.maxCache(), po.getPkey(),
                    autoTrans.namespace(), TransType.AUTO_TRANS);
        }
        return tempCacheTransMap;
    }

    /**
     * 获取用于翻译的缓存
     *
     * @param namespace namespace
     * @param pkey      主键
     * @return 缓存
     */
    private Map<String, Object> getTempTransCacheMap(String namespace, Object pkey) {
        AutoTrans autoTrans = this.transSettMap.get(namespace);
        //如果内存缓存中有,则优先用内存缓存
        if (localTransCacheMap.containsKey(namespace + "_" + pkey)) {
            return localTransCacheMap.get(namespace + "_" + pkey);
        } else if (this.transSettMap.get(namespace).globalCache() && getFromGlobalCache(pkey, namespace, TransType.AUTO_TRANS) != null) {
            return getFromGlobalCache(pkey, namespace, TransType.AUTO_TRANS);
        }
        //如果注解为空,代表可能是其他的服务提供的翻译,尝试去redis获取缓存
        else if (autoTrans == null || autoTrans.useRedis()) {
            Map<String, Object> redisCacheResult = this.getRedisTransCache().get(namespace + "_" + pkey);
            //如果获取到了返回
            if (redisCacheResult != null) {
                return redisCacheResult;
            }
            //redis获取不到返回空map
            return new HashMap<>();
        } else {
            //如果强调使用缓存,则可能是还没刷新进来,直接返回空map,前端在刷新一下就好了
            if (autoTrans.useCache()) {
                return new HashMap<>();
            }
            if (this.threadLocalCache.get() == null) {
                if (CheckUtils.isNullOrEmpty(pkey)) {
                    return new HashMap<>();
                }
                VO vo = findById(() -> {
                    return this.baseServiceMap.get(namespace).selectById(pkey);
                }, null);
                return createTempTransCacheMap(vo, autoTrans);
            }
            return this.threadLocalCache.get().get(namespace + "_" + pkey);
        }
    }

    /**
     * 翻译单个的key
     *
     * @param namespace namespace
     * @param pkeyVal   主键
     * @return
     */
    public String transKey(String namespace, String pkeyVal) {
        Map<String, Object> tempCacheTransMap = localTransCacheMap.get(namespace + "_" + pkeyVal);
        if (tempCacheTransMap == null) {
            LOGGER.error("auto trans缓存未命中:" + namespace + "_" + pkeyVal);
        } else {
            for (String key : tempCacheTransMap.keySet()) {
                return ConverterUtils.toString(tempCacheTransMap.get(key));
            }
        }
        return null;
    }

    /**
     * 类扫描器
     *
     * @param annotationClass 注解
     * @param packageNames    包
     * @return 符合条件的类
     */
    public static Set<Class<?>> scan(Class<? extends Annotation> annotationClass, String[] packageNames) {
        TypeFilter entityFilter = AnnotationTypeFilterBuilder.build(annotationClass);
        SpringClassScanner entityScanner = new SpringClassScanner.Builder().typeFilter(entityFilter).build();
        for (String packageName : packageNames) {
            entityScanner.getScanPackages().add(packageName);
        }
        Set<Class<?>> entitySet = null;
        try {
            entitySet = entityScanner.scan();
        } catch (ClassNotFoundException | IOException e) {
            LOGGER.error("包扫描错误", e);
            // log or throw runTimeExp
            throw new RuntimeException(e);
        }
        return entitySet;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        init(applicationReadyEvent);
    }

    public void setPackageNames(String packageNames) {
        this.packageNames = packageNames;
    }

    public void setRedisTransCache(RedisCacheService<Map<String, Object>> redisTransCache) {
        this.redisTransCache = redisTransCache;
    }

    public void regTransable(AutoTransAble transAble, AutoTrans autoTransSett) {
        this.baseServiceMap.put(autoTransSett.namespace(), transAble);
        this.transSettMap.put(autoTransSett.namespace(), autoTransSett);
    }
}

/**
 * 被翻译的字段的实体
 */
@Data
class TransFieldSett {
    /**
     * trans注解
     */
    private Trans trans;
    /**
     * 命名空间
     */
    private String namespace;
    /**
     * 别名
     */
    String alias;

    public TransFieldSett(Field transField) {
        transField.setAccessible(true);
        trans = transField.getAnnotation(Trans.class);
        namespace = trans.key();
        // 如果是 good#student  翻译出来应该是 goodStuName goodStuAge  customer#customer  customerName
        if (namespace.contains("#")) {
            alias = namespace.substring(namespace.indexOf("#") + 1);
            namespace = namespace.substring(0, namespace.indexOf("#"));
        }
        //如果别名不等于空不等于null的话则使用此别名
        if (trans.alias() != null && (!"".equals(trans.alias()))) {
            alias = trans.alias();
        }
    }


}
