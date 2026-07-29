package com.fhs.trans.vo;

import lombok.Data;

import java.util.List;

/**
 * 根据id查询
 */
@Data
public class FindByIdsQueryPayload {
    private List<String> ids;
    private String uniqueField;
}
