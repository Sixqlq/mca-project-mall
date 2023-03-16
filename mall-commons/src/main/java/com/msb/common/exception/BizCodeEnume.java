package com.msb.common.exception;

/**
 * 错误编码和错误信息的枚举类
 * 通用的错误列表，响应的编码统一为5位数字，前面两位约定为业务场景，最后三位约定为错误码
 * 10：表示通用
 * /000:未知异常 10000
 * /001:参数格式错误 10001
 * 11：商品
 * 12：订单
 * 13：购物车
 * 14：物流
 * 15: 会员
 */
public enum BizCodeEnume {
    UNKNOW_EXCEPTION(10000,"系统未知异常"),
    VALID_EXCEPTION(10001,"参数格式异常"),
    VALID_SMS_EXCEPTION(10002, "短信发送频率太高，稍等一会再发送"),
    PRODUCT_UP_EXCEPTION(11001, "商城上架异常"),
    NO_STOCK_EXCEPTION(14001, "商品锁定库存失败"),
    USERNAME_EXIST_EXCEPTION(15001, "用户名存在"),
    PHONE_EXIST_EXCEPTION(15002, "手机号存在"),
    USERNAME_PHONE_VALID_EXCEPTION(15003, "账号或者密码错误");


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
