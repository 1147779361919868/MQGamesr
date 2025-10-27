package com.fhs.common.constant;

import com.fhs.trans.ds.DataSourceSetter;

public class TransConfig {
    /**
     * 多数据源
     */
    public static boolean MULTIPLE_DATA_SOURCES = false;

    /**
     * 数据库切换
     */
    public static DataSourceSetter dataSourceSetter;
}
