package com.msb.mall.member.exception;

/**
 * 手机号存储的自定义异常
 */
public class PhoneExistException extends RuntimeException{
    public PhoneExistException(){
        super("手机号已存在");
    }
}
