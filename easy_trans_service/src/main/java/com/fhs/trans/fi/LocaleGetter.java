package com.fhs.trans.fi;

/**
 * 国际化
 */
@FunctionalInterface
public interface LocaleGetter {

    /**
     * 获取语言标记
     *
     * @return
     */
    String getLanguageTag();
}
