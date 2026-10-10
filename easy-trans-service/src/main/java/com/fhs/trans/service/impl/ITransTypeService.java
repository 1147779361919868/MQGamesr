package com.fhs.trans.service.impl;


import com.fhs.common.constant.TransConfig;
import com.fhs.common.utils.CheckUtils;
import com.fhs.common.utils.StringUtil;
import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

/**
 * 翻译接口,将此接口实现类注册到transservice即可用
 *
 * @author wanglei
 */
public interface ITransTypeService {

    Logger Logger = LoggerFactory.getLogger(ITransTypeService.class);

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
        if (CheckUtils.isNotEmpty(trans.refs())) {
            Stream.of(trans.refs()).forEach(ref -> ReflectUtils.setValue(vo, ref, val));
        }
    }

    default void setRef(Trans trans, VO vo, Map<String, String> valMap) {
        if (CheckUtils.isNotEmpty(trans.ref())) {
            setRef(trans.ref(), vo, valMap);
        }
        if (CheckUtils.isNotEmpty(trans.refs())) {
            for (int i = 0; i < trans.refs().length; i++) {
                setRef(trans.refs()[i], vo, valMap,i);
            }
        }
    }

    default void setRef(Trans trans, VO vo, Map<String, String> valMap, VO target) {
        if (CheckUtils.isNotEmpty(trans.ref())) {
            boolean isSetRef = false;
            if (target != null) {
                Field field = ReflectUtils.getDeclaredField(vo.getClass(), trans.ref());
                if (field.getType() == target.getClass()) {
                    isSetRef = true;
                    ReflectUtils.setValue(vo, trans.ref(), target);
                }
            }
            //没有set才去set
            if (!isSetRef) {
                setRef(trans.ref(), vo, valMap);
            }
        }
        if (CheckUtils.isNotEmpty(trans.refs())) {
            for (int i = 0; i < trans.refs().length; i++) {
                setRef(trans.refs()[i], vo, valMap, i);
            }
        }
    }

    default void setRef(String ref, VO vo, Map<String, String> valMap) {
        setRef(ref, vo, valMap, null);
    }


    default void setRef(String ref, VO vo, Map<String, String> valMap, Integer index) {
        if (index == null) {
            if (valMap.size() > 0) {
                String key = valMap.keySet().iterator().next();
                ReflectUtils.setValue(vo, ref, valMap.get(key));
            }
        } else {
            ReflectUtils.setValue(vo, ref, valMap.get(new ArrayList<>(valMap.keySet()).get(index)));
        }
    }

    /**
     * 支持多库
     *
     * @param callable
     * @param dataSourceName
     * @return
     */
    default List<? extends VO> findByIds(Callable<List<? extends VO>> callable, String dataSourceName) {
        if (!TransConfig.MULTIPLE_DATA_SOURCES) {
            try {
                return callable.call();
            } catch (Exception e) {
                Logger.error("", e);
            }
            return null;
        }
        CompletableFuture<List<? extends VO>> cf = CompletableFuture.supplyAsync(() -> {
            try {
                if (!StringUtil.isEmpty(dataSourceName)) {
                    TransConfig.dataSourceSetter.setDataSource(dataSourceName);
                }
                return callable.call();
            } catch (Exception e) {
                Logger.error("", e);
            }
            return null;
        });
        try {
            return cf.get();
        } catch (InterruptedException e) {
            Logger.error("", e);
        } catch (ExecutionException e) {
            Logger.error("", e);
        }
        return null;
    }

    /**
     * 支持多库
     *
     * @param callable
     * @param dataSourceName
     * @return
     */
    default VO findById(Callable<VO> callable, String dataSourceName) {
        if (!TransConfig.MULTIPLE_DATA_SOURCES) {
            try {
                return callable.call();
            } catch (Exception e) {
                Logger.error("", e);
            }
            return null;
        }
        CompletableFuture<VO> cf = CompletableFuture.supplyAsync(() -> {
            try {
                if (!StringUtil.isEmpty(dataSourceName)) {
                    TransConfig.dataSourceSetter.setDataSource(dataSourceName);
                }
                return callable.call();
            } catch (Exception e) {
                Logger.error("", e);
            }
            return null;
        });
        try {
            return cf.get();
        } catch (InterruptedException e) {
            Logger.error("", e);
        } catch (ExecutionException e) {
            Logger.error("", e);
        }
        return null;
    }
}
