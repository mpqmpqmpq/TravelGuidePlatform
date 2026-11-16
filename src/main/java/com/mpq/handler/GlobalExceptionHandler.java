package com.mpq.handler;

import com.mpq.common.Result;
import io.jsonwebtoken.JwtException;
import kotlin.io.AccessDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理JWT相关异常
    @ExceptionHandler(JwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result handleJwtAuthenticationException(JwtException e) {
        log.warn("JWT认证异常: {}", e.getMessage());
        return Result.failure(401, "登录已过期，请重新登录");
    }

    // 处理权限不足异常
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result handleAccessDeniedException(AccessDeniedException e) {
        log.warn("访问被拒绝: {}", e.getMessage());
        return Result.failure(403, "没有访问权限");
    }

    // 处理其他未捕获异常
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleGlobalException(Exception e) {
        log.error("服务器内部错误: ", e);
        return Result.failure(500, "服务器内部错误: " + e.getMessage());
    }
}
