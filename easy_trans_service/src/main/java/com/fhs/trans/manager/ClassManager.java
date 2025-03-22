package com.fhs.trans.manager;

import com.fhs.exception.ParamException;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 类管理器
 *
 * @author user
 * @date 2020-05-19 11:15:23
 */
public class ClassManager implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Logger LOGGER = LoggerFactory.getLogger(ClassManager.class);

    private static final Map<String, ClassInfo> CACHE = new HashMap<String, ClassInfo>();

    public static ClassInfo getClassInfoByName(Class<?> clazz) {
        ClassInfo temp = CACHE.get(clazz.getName());
        ClassInfo info = null;
        if (null == temp) {
            try {
                temp = new ClassInfo(clazz);
            } catch (InstantiationException | IllegalAccessException e) {
                LOGGER.error(clazz.getName() + "生成classinfo错误", e);
                throw new ParamException(clazz.getName() + "生成classinfo错误");
            }
            setClassInfoByName(clazz.getName(), temp);
        }
        try {
            info = new ClassInfo();
            BeanUtils.copyProperties(info, temp);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
        return info;
    }

    public static void setClassInfoByName(String className, ClassInfo info) {
        CACHE.put(className, info);
    }

    public static void reMoveDbTableByName(String className) {
        CACHE.remove(className);
    }

    public static void main(String[] args) {
        ClassManager.getClassInfoByName(Long.class);
    }
}
