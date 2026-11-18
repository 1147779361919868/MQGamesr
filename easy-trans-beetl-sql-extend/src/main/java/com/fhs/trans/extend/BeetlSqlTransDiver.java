package com.fhs.trans.extend;

import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.SimpleTransService;
import lombok.extern.slf4j.Slf4j;
import org.beetl.sql.clazz.kit.BeetlSQLException;
import org.beetl.sql.core.SQLManager;

import java.io.Serializable;
import java.util.List;

/**
 * beetl sql 简单翻译驱动
 */
@Slf4j
public class BeetlSqlTransDiver implements SimpleTransService.SimpleTransDiver {

    private SQLManager sqlManager;

    public BeetlSqlTransDiver(SQLManager sqlManager){
        this.sqlManager = sqlManager;
    }

    @Override
    public List<? extends VO> findByIds(List<? extends Serializable> ids, Class<? extends VO> targetClass, String uniqueField) {
        return sqlManager.selectByIds(targetClass,ids);
    }

    @Override
    public VO findById(Serializable id, Class<? extends VO> targetClass, String uniqueField) {
        try {
            return sqlManager.unique(targetClass,id);
        }catch (BeetlSQLException ex){
            log.error(targetClass + " 根据id:" + id + "没有查询到数据");
        }
        return null;
    }
}
