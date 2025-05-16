package com.fhs.trans.json;

import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * 自定义jackson消息转换器---
 * todo  此类未实现功能
 *
 */
public class TransMappingJackson2HttpMessageConverter extends MappingJackson2HttpMessageConverter {

    protected void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        super.writeInternal(object,type,outputMessage);
    }

}
