package com.fhs.trans.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.fhs.common.utils.ConverterUtils;
import com.fhs.common.utils.StringUtil;
import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.constant.TransType;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.listener.TransMessageListener;
import com.fhs.trans.vo.BasicVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 远程翻译服务
 */
@Data
@Slf4j
public class RpcTransService extends SimpleTransService {

    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SimpleTransService.class);

    private RestTemplate restTemplate;


    @Value("${easy-trans.is-enable-cloud:true}")
    private Boolean isEnableCloud;

    /**
     * 用来全局配置其他微服务的ContextPath
     */
    private Map<String, String> serviceContextPathConfigMap = new HashMap<>();

    /**
     * 添加ContextPath 配置
     *
     * @param serviceName 服务名称
     * @param contextPath 服务路径
     */
    public void addContextPath(String serviceName, String contextPath) {
        this.serviceContextPathConfigMap.put(serviceName, contextPath);
    }

    @Override
    public List<? extends VO> findByIds(List ids, Trans tempTrans, Set<String> targetFields) {
        //如果没开启springcloud 则走SimpleTransService逻辑
        if (!isEnableCloud) {
            try {
                Class clazz = Class.forName(tempTrans.targetClassName());
                return findByIds(() -> {
                    return transDiver.findByIds(ids, clazz, tempTrans.uniqueField(), targetFields);
                }, tempTrans.dataSource());

            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("类找不到：" + tempTrans.targetClassName());
            }
        }
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("ids", ids);
        paramMap.put("uniqueField", tempTrans.uniqueField());
        if (targetFields != null) {
            paramMap.put("targetFields", targetFields);
        }
        //执行远程调用
        try {
            String respJson = restTemplate.postForObject("http://" + tempTrans.serviceName()
                    + getContextPath(tempTrans)
                    + "/easyTrans/proxy/" + tempTrans.targetClassName() + "/findByIds", paramMap, String.class);
            return JSONArray.parseArray(respJson, BasicVO.class);
        } catch (Exception e) {
            log.error("trans service执行RPC Trans 远程调用错误:" + tempTrans.serviceName(), e);
        }
        return new ArrayList<>();
    }

    @Override
    public VO findById(Object id, Trans tempTrans) {
        if (!isEnableCloud) {
            try {
                Class clazz = Class.forName(tempTrans.targetClassName());
                return findById(() -> {
                    return transDiver.findById((Serializable) id, clazz, tempTrans.uniqueField(), new HashSet<>(Arrays.asList(tempTrans.fields())));
                }, tempTrans.dataSource());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("类找不到：" + tempTrans.targetClassName());
            }
        }
        //执行远程调用
        try {
            return restTemplate.getForObject("http://" + tempTrans.serviceName()
                    + getContextPath(tempTrans)
                    + "/easyTrans/proxy/" + tempTrans.targetClassName()
                    + "/findById/" + id + "?uniqueField=" + tempTrans.uniqueField() + "&targetFields=" + Arrays.stream(tempTrans.fields()).collect(Collectors.joining(",")), BasicVO.class);
        } catch (Exception e) {
            log.error("trans service执行RPC Trans 远程调用错误:" + tempTrans.serviceName(), e);
        }
        return null;
    }

    /**
     * 获取微服务的contextpath
     *
     * @param tempTrans trans配置
     * @return contextpath
     */
    private String getContextPath(Trans tempTrans) {
        if (serviceContextPathConfigMap.containsKey(tempTrans.serviceName())) {
            return serviceContextPathConfigMap.get(tempTrans.serviceName());
        }
        return StringUtil.isEmpty(tempTrans.serviceContextPath()) ? "" : tempTrans.serviceContextPath();
    }

    /**
     * 获取唯一键
     *
     * @param vo        vo
     * @param tempTrans 翻译注解
     * @return
     */
    public Object getUniqueKey(VO vo, Trans tempTrans) {
        if (!isEnableCloud) {
            return super.getUniqueKey(vo, tempTrans);
        }
        if (StringUtil.isEmpty(tempTrans.uniqueField())) {
            return vo.getPkey();
        }
        BasicVO basicVO = (BasicVO) vo;
        return basicVO.getObjContentMap().get(tempTrans.uniqueField());
    }

    /**
     * 创建一个临时缓存map
     *
     * @param po    po
     * @param trans 配置
     * @return
     */
    protected Map<String, Object> createTempTransCacheMap(VO po, Trans trans) {
        if (!isEnableCloud) {
            return super.createTempTransCacheMap(po, trans);
        }
        String fielVal = null;
        Map<String, Object> tempCacheTransMap = new LinkedHashMap<>();
        if (po == null) {
            return tempCacheTransMap;
        }
        BasicVO basicVO = (BasicVO) po;
        for (String field : trans.fields()) {
            fielVal = ConverterUtils.toString(basicVO.getObjContentMap().get(field));
            tempCacheTransMap.put(field, fielVal);
        }
        if (transCacheSettMap.containsKey(trans.targetClassName())) {
            TransCacheSett cacheSett = transCacheSettMap.get(trans.targetClassName());
            put2GlobalCache(tempCacheTransMap, cacheSett.isAccess(), cacheSett.getCacheSeconds(), cacheSett.getMaxCache(), po.getPkey(),
                    trans.targetClassName(), TransType.RPC);
        }
        return tempCacheTransMap;
    }

    /**
     * 配置缓存
     *
     * @param type
     * @param cacheSett
     */
    public void setTransCache(Object type, TransCacheSett cacheSett) {
        this.transCacheSettMap.put(ConverterUtils.toString(type), cacheSett);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        TransService.registerTransType(TransType.RPC, this);
        //注册刷新缓存服务
        TransMessageListener.regTransRefresher(TransType.RPC, this::onMessage);
    }

}
