package com.fhs.trans.extend;

import com.fhs.common.spring.SpringContextUtil;
import com.fhs.core.trans.anno.AutoTrans;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.AutoTransable;
import com.fhs.trans.service.impl.AutoTransService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 用来注册
 */
@Data
public class JPATransableRegister implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * service的包路径
     */
    private String packageNames;

    @Autowired
    private AutoTransService autoTransService;


    @Autowired
    private EntityManager em;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        //spring容器初始化完成之后，就会自行此方法。
        Set<Class<?>> entitySet = AutoTransService.scan(AutoTrans.class, packageNames.split(";"));
        // 遍历所有class，获取所有用@autowareYLM注释的字段
        if (entitySet != null) {
            final List<String> namespaceList = new ArrayList<>();
            for (Class<?> entity : entitySet) {
                AutoTrans autoTransSett = entity.getAnnotation(AutoTrans.class);
                if (autoTransSett.ref() == VO.class || (!autoTransSett.ref().isAnnotationPresent(Entity.class))) {
                    continue;
                }
                // 获取该类
                Object baseService = SpringContextUtil.getBeanByClass(entity);
                if ((baseService instanceof AutoTransable)) {
                    continue;
                }
                namespaceList.add(autoTransSett.namespace());
                autoTransService.regTransable(new JPATransableAdapter(autoTransSett.ref(), em), autoTransSett);
            }
            new Thread(() -> {
                Thread.currentThread().setName("refresh auto trans cache");
                for (String namespace : namespaceList) {
                    autoTransService.refreshOneNamespace(namespace);
                }
            }).start();

        }
    }

}
