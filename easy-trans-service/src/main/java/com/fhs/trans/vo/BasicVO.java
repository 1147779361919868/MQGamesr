package com.fhs.trans.vo;

import com.fhs.core.trans.vo.VO;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 代理的vo
 */
@Data
public class BasicVO implements VO {

    private String id;

    /**
     * 实际内容map
     */
    private Map<String, Object> objContentMap = new HashMap<>();


}
