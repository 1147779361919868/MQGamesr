package com.fhs.core.trans.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 反射工具类
 *
 * @author jackwang
 * @version [版本号, 2015年8月7日]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
@Slf4j
public class ReflectUtils {


    /**
     * 调用obj的setfield 方法设置value
     *
     * @param obj       obj
     * @param fieldName 字段
     * @param value     值
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void setValue(Object obj, String fieldName, Object value) {
        Field field = null;
        try {
            if (obj instanceof Map) {
                ((Map) obj).put(fieldName, value);
                return;
            }
            field = getDeclaredField(obj.getClass(), fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            log.info("给" + obj + "的字段" + fieldName + "设置值" + value + "错误", e);
        }
    }

    /**
     * 给class的field设置值
     *
     * @param obj   obj
     * @param field field
     * @param value value
     */
    public static void setValue(Object obj, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(obj, value);
        } catch (SecurityException e) {
            log.error("", e);
        } catch (IllegalArgumentException e) {
            log.error("", e);
        } catch (IllegalAccessException e) {
            log.error("", e);
        }
    }

    /**
     * 把class 反射为一个对象
     *
     * @param cls
     * @return
     */
    public static Object newInstance(Class<?> cls) {
        try {
            return cls.newInstance();
        } catch (Exception ex) {
            log.error("", ex);
        }
        return null;
    }

    /**
     * 通过Class<?>获取该对象下面所有的属性
     *
     * @param clas 对象
     * @return 属性map
     */
    public static Map<String, Object> getClassFiledMap(Class<?> clas) {
        Map<String, Object> map = new HashMap<String, Object>();
        final Field[] fields = clas.getDeclaredFields();
        for (Field field : fields) {
            map.put(field.getName(), field.getType());
        }
        return map;
    }

    /**
     * 循环向上转型, 获取对象的 DeclaredField
     *
     * @param clazz     : 子类对象
     * @param fieldName : 父类中的属性名
     * @return 父类中的属性对象
     */

    public static Field getDeclaredField(Class<?> clazz, String fieldName) {
        Field field = null;

        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                field = clazz.getDeclaredField(fieldName);
                return field;
            } catch (Exception e) {
                // 这里甚么都不要做！并且这里的异常必须这样写，不能抛出去。
                // 如果这里的异常打印或者往外抛，则就不会执行clazz = clazz.getSuperclass(),最后就不会进入到父类中了

            }
        }

        return null;
    }

    /**
     * 获取对象的所有field
     *
     * @param object object
     * @return 所有字段
     */
    public static List<Field> getAllField(Object object) {
        Class<?> clazz = object.getClass();

        return getAllField(clazz);
    }

    /**
     * 获取一个class的所有的字段
     *
     * @param clazz class
     * @return 所有字段
     */
    public static List<Field> getAllField(Class clazz) {
        Field[] fields = null;
        List<Field> result = new ArrayList<Field>();
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                fields = clazz.getDeclaredFields();
                result.addAll(Arrays.asList(fields));
            } catch (Exception e) {
                // 这里甚么都不要做！并且这里的异常必须这样写，不能抛出去。
                // 如果这里的异常打印或者往外抛，则就不会执行clazz = clazz.getSuperclass(),最后就不会进入到父类中了
            }
        }
        return result;
    }


    /**
     * 反射获取一个值
     *
     * @param obj       obj
     * @param fieldName 字段名称
     * @return obj.字段 的值
     */
    @SuppressWarnings("rawtypes")
    public static Object getValue(Object obj, String fieldName) {
        Field field;
        try {
            if (obj instanceof Map) {
                Object result = ((Map) obj).get(fieldName);
                return result;
            }

            List<Field> declaredfields = getAllField(obj.getClass());
            for (int i = 0; i < declaredfields.size(); i++) {
                if (declaredfields.get(i).getName().equals(fieldName)) {
                    field = declaredfields.get(i);
                    field.setAccessible(true);
                    return field.get(obj);
                }
            }
        } catch (SecurityException e) {
            log.error("", e);
        } catch (IllegalArgumentException e) {
            log.error("", e);
        } catch (IllegalAccessException e) {
            log.error("", e);
        }
        return "";
    }


    /**
     * 反射获取一个值
     *
     * @param obj       obj
     * @param fieldName 字段名称
     * @return obj.字段 的值
     */
    @SuppressWarnings("rawtypes")
    public static Object getValueFromAllFields(Object obj, String fieldName, int fatherIndex) {
        Field field;
        try {
            if (obj instanceof Map) {
                Object result = ((Map) obj).get(fieldName);
                return result;
            }

            Class<? extends Object> superClass = obj.getClass();
            for (int i = 0; i < fatherIndex; i++) {
                superClass = superClass.getSuperclass();
            }
            Field[] superDeclaredfields = superClass.getDeclaredFields();

            for (int i = 0; i < superDeclaredfields.length; i++) {
                if (superDeclaredfields[i].getName().equals(fieldName)) {
                    field = superDeclaredfields[i];
                    field.setAccessible(true);
                    return field.get(obj);
                }
            }
        } catch (SecurityException e) {
            log.error("", e);
        } catch (IllegalArgumentException e) {
            log.error("", e);
        } catch (IllegalAccessException e) {
            log.error("", e);
        }
        return "";
    }

    /**
     * 判断字段是否存在
     *
     * @param obj       obj
     * @param fieldName 字段名字
     * @return true存在 false不存在
     */
    @SuppressWarnings("rawtypes")
    public static boolean checkFiledIsExit(Object obj, String fieldName) {

        if (obj instanceof Map) {
            return ((Map) obj).containsKey(fieldName);
        }
        Field field = getDeclaredField(obj.getClass(), fieldName);
        // 如果field !=null 代表包含fieldName
        return field != null;
    }


    /**
     * 根据一个class和注解获取字段集合
     *
     * @param clazz           class
     * @param annotationClass 注解
     * @return 字段集合
     */
    public static <T extends Annotation> List<Field> getAnnotationField(Class<?> clazz, Class<T> annotationClass) {
        List<Field> result = new ArrayList<>();
        List<Field> fields = getAllField(clazz);
        for (Field field : fields) {
            if (field.getAnnotation(annotationClass) != null) {
                result.add(field);
            }
        }
        return result;
    }


    /**
     * 判断字段是否存在
     *
     * @param clazz     classs
     * @param fieldName 字段名字
     * @return true存在 false不存在
     */
    public static boolean checkFiledIsExit(Class<?> clazz, String fieldName) {
        if (clazz == Map.class) {
            return true;
        }
        Field field = getDeclaredField(clazz, fieldName);
        // 如果field !=null 代表包含fieldName
        return field != null;
    }

    /**
     * 获取所有的方法
     *
     * @param clazz 需要获取所有方法的class
     * @return 所有的方法
     */
    public static List<Method> getAllMethod(Class<?> clazz) {
        return Arrays.asList(clazz.getMethods());
    }

    /**
     * 根据名称获取method
     *
     * @param clazz class
     * @param name  方法名称
     * @return
     */
    public static Method getMethodd(Class<?> clazz, String name) {
        List<Method> methods = Arrays.asList(clazz.getMethods()).stream().filter(method -> {
            return name.equals(method.getName());
        }).collect(Collectors.toList());
        return methods.isEmpty() ? null : methods.get(0);
    }
}
