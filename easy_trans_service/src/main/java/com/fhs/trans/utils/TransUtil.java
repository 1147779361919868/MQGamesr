package com.fhs.trans.utils;

import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.TransService;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.beans.BeanGenerator;
import net.sf.cglib.beans.BeanMap;
import org.apache.commons.beanutils.PropertyUtilsBean;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Slf4j
public class TransUtil {


    public static Object transResult(Object result, TransService transService) {
        if(result ==null){
            return null;
        }
        if(result instanceof Collection){
            if(((Collection)result).isEmpty()){
                return result;
            }
            if(((Collection)result).iterator().next() instanceof VO){
                transService.transMore((List<? extends VO>) result);
                return result;
            }
        }
        List<VO> transOneVOs = getTransOneVOs(result);
        for (VO transOneVO : transOneVOs) {
            transService.transOne(transOneVO);
        }
        List<List<VO>> voLists = getTransMoreVOS(result);
        for (List<VO> voList : voLists) {
            // 保证集合里的数据是vo
            if (!voList.isEmpty() && voList.get(0) instanceof VO) {
                transService.transMore(voList);
            }
        }
        return result;
    }

    /**
     * 获取单个vo
     *
     * @param proceed
     * @return
     */
    private static List<VO> getTransOneVOs(Object proceed) {
        List<VO> result = new ArrayList<>();
        if (proceed == null) {
            return result;
        }
        if (VO.class.isAssignableFrom(proceed.getClass())) {
            result.add((VO) proceed);
        }
        List<Field> fields = ReflectUtils.getAllField(proceed);
        VO vo = null;
        for (Field field : fields) {
            if (checkFieldIsVO(field, proceed)) {
                try {
                    field.setAccessible(true);
                    vo = (VO) field.get(proceed);
                    if (vo != null) {
                        result.add(vo);
                    }
                } catch (IllegalAccessException e) {
                    log.error("获取值错误", e);
                }
            }
        }

        return result;
    }

    /**
     * 创建代理对象
     *
     * @param obj
     * @return
     */
    public static Object createProxyForJackson(Object obj) throws IllegalAccessException, InstantiationException {
        if (obj == null) {
            return null;
        }
        if (checkIsVOCollection(obj)) {
            return  createVOProxyCollection((Collection) obj);
        }
        Object result = obj;
        List<Field> fields = ReflectUtils.getAllField(obj);
        VO vo = null;
        for (Field field : fields) {
            field.setAccessible(true);
            if (checkFieldIsVO(field, obj)) {
                try {
                    vo = (VO) field.get(obj);
                    if (vo != null) {
                        field.set(obj, createProxyVoForJackson(vo));
                    }
                } catch (IllegalAccessException e) {
                    log.error("获取值错误", e);
                }
            } else if (checkIsVOCollection(field.get(obj))) {//如果是vo集合则创建vo集合
                field.set(obj, createVOProxyCollection((Collection) field.get(obj)));
            }
        }
        if (obj instanceof VO) {
            result = createProxyVoForJackson((VO) obj);
        }
        if (checkIsVOCollection(obj)) {
            result = createVOProxyCollection((Collection) obj);
        }
        return result;
    }

    /**
     * 创建vo代理集合
     *
     * @param collection
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static Collection createVOProxyCollection(Collection collection) throws InstantiationException, IllegalAccessException {
        Collection result = null;
        if(collection instanceof List){
            result = new ArrayList();
        }else if(collection instanceof Set){
            result = new HashSet();
        }else{
            return collection;
        }
        for (Object vo : collection) {
            result.add(createProxyVoForJackson((VO) vo));
        }
        return result;
    }

    /**
     * 判断一个对象是不是vo集合
     *
     * @param obj
     * @return
     */
    public static boolean checkIsVOCollection(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Collection) {
            Collection collection = (Collection) obj;
            if (collection.isEmpty()) {
                return false;
            }
            Object canVo = collection.iterator().next();
            if (canVo instanceof VO) {
                return true;
            }
        }
        return false;
    }

    /**
     * 创建新 vo
     *
     * @param vo
     * @return
     */
    public static Object createProxyVoForJackson(VO vo) {
        if (vo == null || vo.getTransMap() == null) {
            return vo;
        }

        Map<String, Class> propertyMap = new HashMap<String, Class>();
        for (String key : vo.getTransMap().keySet()) {
            propertyMap.put(key, String.class);
        }
        vo.getTransMap().put("transMap", null);
        try {
            Map transMap = vo.getTransMap();
            return PropertyAppender.generate(vo, transMap);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return vo;
    }


    /**
     * 判断一个是否vo
     *
     * @param field
     * @return
     */
    public static boolean checkFieldIsVO(Field field, Object proceed) {
        Class clazz = getFieldClass(field, proceed);
        if (clazz == null) {
            return false;
        }
        if (VO.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }


    /**
     * 获取trans more vos
     *
     * @param proceed
     * @return
     */
    private static List<List<VO>> getTransMoreVOS(Object proceed) {
        List<List<VO>> result = new ArrayList<>();
        if (proceed == null) {
            return result;
        }
        if (Collection.class.isAssignableFrom(proceed.getClass())) {
            result.add(new ArrayList<>((Collection) proceed));
        }
        List<Field> fields = ReflectUtils.getAllField(proceed);
        List voList = null;
        for (Field field : fields) {
            if (checkFieldIsCollection(field, proceed)) {
                field.setAccessible(true);
                try {
                    Collection collection = (Collection) field.get(proceed);
                    if (collection == null || collection.isEmpty()) {
                        continue;
                    }
                    voList = new ArrayList();
                    voList.addAll(collection);
                    result.add(voList);
                } catch (IllegalAccessException e) {
                    log.error("", e);
                }
            }
        }
        return result;
    }

    public static Class getFieldClass(Field field, Object proceed) {
        String className = field.getGenericType().getTypeName().replace("class ", "");
        if ("T".equals(className)) {
            try {
                field.setAccessible(true);
                Object obj = field.get(proceed);
                if (obj == null) {
                    return Object.class;
                }
                if (VO.class.isAssignableFrom(obj.getClass())) {
                    return DefaultVO.class;
                }
                if (Collection.class.isAssignableFrom(obj.getClass())) {
                    return List.class;
                }
            } catch (IllegalAccessException e) {
                log.error("", e);
                return Object.class;
            }
        }
        //不支持数组 基础数据类型和内部类
        if (className.contains("<")) {
            className = className.substring(0, className.indexOf("<"));
        }
        //不支持数组 基础数据类型和内部类
        if (className.contains("/") ||
                (Character.isLowerCase(className.charAt(0)) && (!className.contains(".")))
                || className.contains("$")) {
            return null;
        }

        if (className.contains("]")) {
            className = className.substring(0, className.indexOf("["));
        }
        if (className.contains("]")) {
            className = className.substring(0, className.indexOf("<"));
        }

        try {
            Class clazz = Class.forName(className);
            return clazz;
        } catch (ClassNotFoundException e) {
            log.error("", e);
        }
        return null;
    }

    /**
     * 判断一个字段是否是集合
     *
     * @param field
     * @return
     */
    public static boolean checkFieldIsCollection(Field field, Object proceed) {
        Class clazz = getFieldClass(field, proceed);
        if (clazz == null) {
            return false;
        }
        if (Collection.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }
}

/**
 * 实际上没什么用，只是用来判断T是否是VO的时候返回此类型
 */
class DefaultVO implements VO {

    @Override
    public Map<String, String> getTransMap() {
        return null;
    }
}


/**
 * 给对象加字段
 */
@Slf4j
class PropertyAppender {

    private static final class DynamicBean {

        private Object target;

        private BeanMap beanMap;

        private DynamicBean(Class superclass, Map<String, Class> propertyMap) {
            this.target = generateBean(superclass, propertyMap);
            this.beanMap = BeanMap.create(this.target);
        }

        private void setValue(String property, Object value) {
            beanMap.put(property, value);
        }

        private Object getValue(String property) {
            return beanMap.get(property);
        }

        private Object getTarget() {
            return this.target;
        }

        /**
         * 根据属性生成对象
         */
        private Object generateBean(Class superclass, Map<String, Class> propertyMap) {
            BeanGenerator generator = new BeanGenerator();
            if (null != superclass) {
                generator.setSuperclass(superclass);
            }
            BeanGenerator.addProperties(generator, propertyMap);
            return generator.create();
        }
    }

    public static Object generate(Object dest, Map<String, Object> newValueMap) throws InvocationTargetException, IllegalAccessException {
        PropertyUtilsBean propertyUtilsBean = new PropertyUtilsBean();

        //1.获取原对象的字段数组
        PropertyDescriptor[] descriptorArr = propertyUtilsBean.getPropertyDescriptors(dest);

        //2.遍历原对象的字段数组，并将其封装到Map
        Map<String, Class> oldKeyMap = new HashMap<>(4);
        for (PropertyDescriptor it : descriptorArr) {
            if (!"class".equalsIgnoreCase(it.getName())) {
                oldKeyMap.put(it.getName(), it.getPropertyType());
                newValueMap.put(it.getName(), it.getReadMethod().invoke(dest));
            }
        }

        //3.将扩展字段Map合并到原字段Map中
        newValueMap.forEach((k, v) -> {
            if (v != null) {
                oldKeyMap.put(k, v.getClass());
            }
        });

        //4.根据新的字段组合生成子类对象
        DynamicBean dynamicBean = new DynamicBean(dest.getClass(), oldKeyMap);

        //5.放回合并后的属性集合
        newValueMap.forEach((k, v) -> {
            try {
                dynamicBean.setValue(k, v);
            } catch (Exception e) {
                log.error("动态添加字段【值】出错", e);
            }
        });
        return dynamicBean.getTarget();
    }
}