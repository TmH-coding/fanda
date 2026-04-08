package com.fanda.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(40000, "请求参数错误"),
    USER_EXISTS(40001, "用户名已存在"),
    BAD_CREDENTIALS(40002, "用户名或密码错误"),
    TOKEN_EXPIRED(40101, "登录已过期"),
    TOKEN_INVALID(40102, "无效的令牌"),
    NOT_FOUND(40400, "资源不存在"),
    FORBIDDEN(40300, "无权限操作"),
    GROUP_FULL(40003, "拼饭人数已满"),
    ALREADY_JOINED(40004, "已经加入该拼饭"),
    ALREADY_VOTED(40005, "已经投过票"),
    SERVER_ERROR(50000, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
