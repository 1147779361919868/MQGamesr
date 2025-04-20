package com.fhs.trans.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.fhs.common.utils.ConverterUtils;
import com.fhs.core.trans.vo.VO;
import lombok.Data;

import javax.persistence.Id;
import java.util.HashMap;
import java.util.Map;

/**
 * 代理的vo
 */
@Data
public class BasicVO implements VO {

    @Id
    @TableId
    private String id;

    /**
     * id字段名称
     */
    private String idFieldName;

    /**
     * 实际内容map
     */
    private Map<String, Object> objContentMap = new HashMap<>();

    public String getId() {
        return ConverterUtils.toString(objContentMap.get(idFieldName));
    }
}
