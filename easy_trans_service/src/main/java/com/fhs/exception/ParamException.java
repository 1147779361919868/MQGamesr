package com.fhs.exception;



/**
 * 参数错误
 * by 王磊
 */
public class ParamException extends RuntimeException   {

    public ParamException(String defaultMessage) {
        super(defaultMessage);
    }

}
