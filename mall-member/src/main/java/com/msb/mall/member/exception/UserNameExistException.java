package com.msb.mall.member.exception;

/**
 * 账号存储的自定义异常
 */
public class UserNameExistException extends RuntimeException {
    public UserNameExistException(){
        super("账号已存在");
    }
}
