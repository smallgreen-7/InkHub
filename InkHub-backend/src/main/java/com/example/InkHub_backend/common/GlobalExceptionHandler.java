package com.example.InkHub_backend.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

// 全局异常处理器：把异常统一转成 R 返回
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 业务异常：原样返回 code + msg
    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusiness(BusinessException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    // 参数校验异常：@Valid 失败时自动抛，返回第一条错误信息
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return R.fail(400, msg);
    }

    // 路径变量/查询参数类型不对（如 /api/articles/abc）：本该 400，别落到 500
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return R.fail(400, "参数不合法：" + e.getName());
    }

    // 缺少必填的请求参数（如 /api/articles/semantic 没带 keyword）
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<?> handleMissingParam(MissingServletRequestParameterException e) {
        return R.fail(400, "缺少参数：" + e.getParameterName());
    }

    // 未知异常：打日志，不把错误详情暴露给前端
    @ExceptionHandler(Exception.class)
    public R<?> handleUnknown(Exception e) {
        log.error("系统异常", e);
        return R.fail(500, "系统繁忙，请稍后再试");
    }

    // 上传文件超过大小限制
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public R<?> handleMaxUpload(MaxUploadSizeExceededException e) {
        return R.fail(400, "文件太大，单文件最大 5MB");
    }
}
