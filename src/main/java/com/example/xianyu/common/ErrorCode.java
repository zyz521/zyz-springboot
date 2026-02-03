package com.example.xianyu.common;

/**
 * 简单的业务错误码枚举
 */
public enum ErrorCode {

    SUCCESS(0, "success"),

    // 通用
    PARAM_ERROR(1000, "参数错误"),
    SYSTEM_ERROR(1001, "系统异常"),

    // 用户相关
    UNAUTHORIZED(2000, "未登录"),
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_OR_PASSWORD_ERROR(2002, "用户名或密码错误"),

    // 商品相关
    PRODUCT_NOT_FOUND(3000, "商品不存在"),

    // 订单相关
    ORDER_ERROR(4000, "订单异常");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}


