package com.fhs.trans.service.impl;




import com.fhs.core.trans.vo.VO;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 翻译接口,将此接口实现类注册到transservice即可用
 * @author wanglei
 */
public interface ITransTypeService
{
    /**
     * 翻译一个字段
     * @param obj 需要翻译的对象
     * @param toTransList 需要翻译的字段
     */
    public void transOne(VO obj, List<Field> toTransList);

    /**
     * 翻译多个 字段
     * @param objList 需要翻译的对象集合
     * @param toTransList 需要翻译的字段集合
     */
    public void transMore(List<? extends VO> objList, List<Field> toTransList);

}
