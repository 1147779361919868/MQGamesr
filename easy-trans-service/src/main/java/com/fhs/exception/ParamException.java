package com.fhs.exception;


/**
 * 参数错误
 * @author wanglei
 */
public class ParamException extends RuntimeException {

    public ParamException(String defaultMessage) {
        super(defaultMessage);
    }

}
