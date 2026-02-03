package com.example.xianyu.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理
 * 只处理 API 请求（返回 JSON），不处理页面请求（返回 HTML）
 */
@RestControllerAdvice
@Order(1) // 设置较低的优先级
public class GlobalExceptionHandler {

    /**
     * 判断是否为 API 请求（期望返回 JSON）
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        String path = request.getRequestURI();
        
        // 如果 Accept 头明确包含 application/json，则认为是 API 请求
        if (accept != null && accept.contains("application/json")) {
            return true;
        }
        
        // 如果 Accept 头包含 text/html，则认为是页面请求，不处理
        if (accept != null && accept.contains("text/html")) {
            return false;
        }
        
        // 对于没有明确 Accept 头的请求，根据路径判断
        // 如果路径包含 /api/ 或 /messages/ 等 API 路径，则认为是 API 请求
        if (path != null && (path.contains("/api/") || path.contains("/messages/") || 
            path.contains("/pond/comment") || path.contains("/pond/like"))) {
            return true;
        }
        
        // 默认情况下，对于页面请求（如 /user/change-password），不处理
        return false;
    }

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e, HttpServletRequest request) {
        // 只处理 API 请求
        if (!isApiRequest(request)) {
            // 对于页面请求，重新抛出异常，让 Spring 的默认异常处理机制处理
            throw e;
        }
        return Result.failure(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        // 只处理 API 请求
        if (!isApiRequest(request)) {
            // 对于页面请求，重新抛出异常，让 Spring 的默认异常处理机制处理
            // 包装为 RuntimeException 因为 MethodArgumentNotValidException 是检查异常
            throw new RuntimeException(e);
        }
        String msg = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : ErrorCode.PARAM_ERROR.getMsg();
        return Result.failure(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOtherException(Exception e, HttpServletRequest request) {
        // 排除资源未找到异常（404），让 Spring 默认处理
        // 包装为 RuntimeException 因为 NoResourceFoundException 是检查异常
        if (e instanceof NoResourceFoundException) {
            throw new RuntimeException(e);
        }
        
        // 只处理 API 请求
        if (!isApiRequest(request)) {
            // 对于页面请求，重新抛出异常，让 Spring 的默认异常处理机制处理
            // 包装为 RuntimeException 以避免类型不匹配
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
        // 简单处理，生产可以加日志
        e.printStackTrace(); // 打印异常堆栈，便于调试
        return Result.failure(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMsg());
    }
}


