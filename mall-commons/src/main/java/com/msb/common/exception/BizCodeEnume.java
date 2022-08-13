package com.msb.common.exception;

/**
 * 错误编码和错误信息的枚举类
 * 通用的错误列表，响应的编码统一为5位数字，前面两位约定为业务场景，最后三位约定为错误码
 * 10：表示通用
 * /000:未知异常 10000
 * /001:参数格式错误 10001
 * 11：商品
 * 12：订单
 * 13：物流
 * 14：会员
 */
public enum BizCodeEnume {
    UNKNOW_EXCEPTION(10000,"系统未知异常"),
    VALID_EXCEPTION(10001,"参数格式异常");

    private int code;
    private String msg;

    BizCodeEnume(int code,String msg){
        this.code = code;
        this.msg = msg;
    }
    public int getCode(){
        return code;
    }

    public String getMsg(){
        return msg;
    }
}
