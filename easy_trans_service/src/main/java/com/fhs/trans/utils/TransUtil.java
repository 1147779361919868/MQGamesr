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

import static com.fhs.core.trans.util.ReflectUtils.getAllField;

@Slf4j
public class TransUtil {


    /**
     * 翻译集合
     *
     * @param object       被翻译的对象
     * @param transService
     * @param isProxy
     * @return
     */
    public static Collection transBatch(Object object, TransService transService, boolean isProxy) throws IllegalAccessException, InstantiationException {
        Collection param = (Collection) object;
        if (param == null) {
            return null;
        }
        if (param.isEmpty()) {
            return param;
        }
        boolean isVo = false;
        if (param.iterator().next() instanceof VO) {
            transService.transMore(new ArrayList<>(param));
            isVo = true;
        } else {
            for (Object tempObject : param) {
                transOne(tempObject, transService, isProxy);
            }
        }
        if (!isProxy || (!isVo)) {
            return (Collection) object;
        }
        Collection result = null;
        if (param instanceof List) {
            result = new ArrayList();
        } else if (param instanceof Set) {
            result = new HashSet();
        } else {
            return param;
        }
        for (Object vo : param) {
            result.add(createProxyVoForJackson((VO) vo));
        }
        return result;
    }


    public static Object transOne(Object object, TransService transService, boolean isProxy) throws IllegalAccessException, InstantiationException {
        if (object == null) {
            return null;
        }
        boolean isVo = false;
        if (object instanceof VO) {
            transService.transOne((VO) object);
            isVo = true;
        } else if (object instanceof Collection) {
            return transBatch(object, transService, isProxy);
        } else if (object.getClass().getName().startsWith("java.")) {
            return object;
        } else {
            List<Field> fields = ReflectUtils.getAllField(object);
            Object tempObj = null;
            for (Field field : fields) {
                if(java.lang.reflect.Modifier.isFinal(field.getModifiers()) || java.lang.reflect.Modifier.isStatic(field.getModifiers())){
                    continue;
                }
                field.setAccessible(true);
                tempObj = field.get(object);
                try{
                    field.set(object, transOne(tempObj, transService, isProxy));
                }catch (Exception e){
                    log.error("如果字段set错误，请反馈给easytrans开发者",e);
                }

            }
        }
        return (isProxy && isVo) ? createProxyVoForJackson((VO) object) : object;
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
        try {
            Map transMap = vo.getTransMap();
            Map copyMap = new HashMap();
            for (Object key : transMap.keySet()) {
                copyMap.put(key, transMap.get(key));
            }
            copyMap.put("transMap", null);
            return PropertyAppender.generate(vo, copyMap);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return vo;
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