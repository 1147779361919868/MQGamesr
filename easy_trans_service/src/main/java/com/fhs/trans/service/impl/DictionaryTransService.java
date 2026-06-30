package com.fhs.trans.service.impl;

import com.fhs.cache.service.FuncGetter;
import com.fhs.cache.service.RedisCacheService;
import com.fhs.common.constant.Constant;
import com.fhs.common.spring.SpringContextUtil;
import com.fhs.common.utils.StringUtil;
import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.constant.TransType;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import com.fhs.exception.ParamException;
import com.fhs.trans.fi.LocaleGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

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
     * redis key前缀
     */
    private static final String TRANS_PRE = "trans:";

    @Autowired(required = false)
    private RedisCacheService<String> redisCacheService;

    /**
     * 用来放字典缓存的map
     */
    private Map<String, String> dictionaryTransMap = new ConcurrentHashMap<>();

    /**
     * 反向翻译
     */
    private Map<String, String> unTransMap = new ConcurrentHashMap<>();

    /**
     * 是否开启国际化
     */
    private boolean isOpenI18n;


    /**
     * 当前国际化语言环境获取
     */
    private LocaleGetter localeGetter;

    /**
     * 是否使用redis 默认false
     */
    private boolean isUseRedis;


    /**
     * 刷新缓存
     *
     * @param dictGroupCode 字典分组编码
     * @param dicMap        字典map
     */
    public void refreshCache(String dictGroupCode, Map<String, String> dicMap) {
        dicMap.keySet().forEach(dictCode -> {
            if (!isUseRedis) {
                dictionaryTransMap.put(dictGroupCode + "_" + dictCode, dicMap.get(dictCode));
            } else {
                redisCacheService.put(TRANS_PRE + dictGroupCode + "_" + dictCode, dicMap.get(dictCode));
            }
            unTransMap.put(dictGroupCode + "_" + dicMap.get(dictCode), dictCode);
        });
    }

    public Map<String, String> getDictionaryTransMap() {
        return dictionaryTransMap;
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
                    if (!isUseRedis) {
                        dicCodeList.add(dictionaryTransMap.get(getMapKey(key, dicCode)));
                    } else {
                        dicCodeList.add(redisCacheService.get(TRANS_PRE + getMapKey(key, dicCode)));
                    }
                }
            }
            String transResult = dicCodeList.size() > Constant.ZERO ? StringUtil.getStrForIn(dicCodeList, false) : "";
            if (obj.getTransMap() != null) {
                obj.getTransMap().put(tempField.getName() + "Name", transResult);
            }
            setRef(tempTrans, obj, transResult);
        }
    }

    /**
     * 获取map翻译的key
     *
     * @param dictGroupCode 字典分组编码
     * @param dictCode      字典编码
     * @return 翻译mapkey
     */
    public String getMapKey(String dictGroupCode, String dictCode) {
        //开启了国际化就拼接国际化
        if (this.isOpenI18n) {
            return dictGroupCode + "_" + dictCode + "_" + this.localeGetter.getLanguageTag();
        }
        return dictGroupCode + "_" + dictCode;
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

    /**
     * 开启国际化
     *
     * @param localeGetter
     */
    public void openI18n(LocaleGetter localeGetter) {
        this.isOpenI18n = true;
        this.localeGetter = localeGetter;
    }

    /**
     * 标记使用redis
     */
    public void makeUseRedis() {
        this.isUseRedis = true;
        if (redisCacheService == null) {
            throw new RuntimeException("使用redis 请将 easy-trans.is-enable-redis 设置为true");
        }
    }

    public Map<String, String> getUnTransMap() {
        return this.unTransMap;
    }

}
