package com.fhs.trans.service;


import com.fhs.core.trans.vo.VO;

import java.util.ArrayList;
import java.util.List;

/**
 * 只有实现了这个接口的才能自动翻译
 * by jackwang
 *
 * @author jackwang
 * @date 2020-05-19 10:26:15
 */
public interface AutoTransable<V extends VO> {

    /**
     * 根据ids查询，过期啦，请使用 selectByIds
     * @param ids
     * @return
     */
    @Deprecated
    default List<V> findByIds(List<? extends Object> ids){
        return new ArrayList<>();
    }

    /**
     * 根据ids查询
     * @param ids
     * @return
     */
    default List<V> selectByIds(List<? extends Object> ids){
        return this.findByIds(ids);
    }

    /**
     * 获取db中所有的数据
     *
     * @return db中所有的数据
     */
    default List<V> select(){
        return new ArrayList<>();
    }

    /**
     * 根据id获取 vo
     *
     * @param primaryValue id
     * @return vo
     */
    V selectById(Object primaryValue);
}
