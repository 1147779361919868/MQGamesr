package com.fhs.trans.extend;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fhs.common.spring.SpringContextUtil;
import com.fhs.core.trans.anno.AutoTrans;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.AutoTransAble;
import com.fhs.trans.service.impl.AutoTransService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 用来注册
 */
@Data
public class MybatisPlusTransableRegister implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * service的包路径
     */
    private String packageNames;

    @Autowired
    private AutoTransService autoTransService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        //spring容器初始化完成之后，就会自行此方法。
        Set<Class<?>> entitySet = AutoTransService.scan(AutoTrans.class, packageNames.split(";"));
        // 遍历所有class，获取所有用@autowareYLM注释的字段
        if (entitySet != null) {
            final List<String> namespaceList = new ArrayList<>();
            for (Class<?> entity : entitySet) {
                AutoTrans autoTransSett = entity.getAnnotation(AutoTrans.class);
                if (autoTransSett.ref() == VO.class || (!autoTransSett.ref().isAnnotationPresent(TableName.class))) {
                    continue;
                }
                // 获取该类
                Object baseService = SpringContextUtil.getBeanByClass(entity);
                if ((baseService instanceof AutoTransAble)) {
                    continue;
                }
                namespaceList.add(autoTransSett.namespace());
                autoTransService.regTransable(new MybatisPlusTransableAdapter(autoTransSett.ref()), autoTransSett);
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
