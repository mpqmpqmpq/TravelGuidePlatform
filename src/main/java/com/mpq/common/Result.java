package com.mpq.common;

import lombok.Data;

@Data
public class Result<T> {
    private int code; // 状态码
    private String message; // 返回信息
    private T data; // 返回数据

    // 成功（带数据）
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    // 成功（不带数据）
    public static <T> Result<T> success(String message) {
        return success(message, null);
    }

    // 失败
    public static <T> Result<T> failure(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }


    public static <T> Result<T> unauthorized(String message) {
        return failure(401, message);
    }
}