package com.campushub.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.ErrorResponse;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError == null ? "参数错误" : fieldError.getDefaultMessage();
        return Result.error(400, msg);
    }

    /**
     * 上传文件超过 spring.servlet.multipart 的限制。
     * 这个异常不是 ErrorResponse，不单独处理会被兜底成「系统繁忙」，让用户以为是服务挂了。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        log.warn("上传文件超过限制：{}", e.getMessage());
        return Result.error(400, "图片过大，请压缩后再上传");
    }

    /**
     * 路径/查询参数类型不对（例如 /orders/abc 里 id 要 Long），
     * 属于请求方写错，必须回 400，不能让兜底处理器报成「系统繁忙」。
     * 请求体不是合法 JSON 同样归到这里。
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class})
    public Result<Void> handleBadRequest(Exception e) {
        log.warn("请求参数不合法：{}", e.getMessage());
        return Result.error(400, "请求参数不合法，请检查后重试");
    }

    /**
     * 兜底处理。
     * 网关同样会加载本类，而网关的「路由不存在(404)」「找不到服务实例(503)」
     * 都实现 ErrorResponse，若一律按 500 处理会把「路径写错 / 服务没启动」
     * 伪装成「系统繁忙」，联调时极难定位。这里按原始状态码给出明确提示，
     * 预期内错误只 warn 不打印堆栈。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        if (e instanceof ErrorResponse errorResponse) {
            int status = errorResponse.getStatusCode().value();
            log.warn("请求异常 {} - {}", status, e.getMessage());
            String msg = switch (status) {
                case 404 -> "接口不存在，请检查请求路径";
                case 405 -> "请求方法不支持";
                case 503 -> "目标服务暂不可用，请确认对应服务已启动并注册到 Nacos";
                default -> "系统繁忙，请稍后重试";
            };
            return Result.error(status, msg);
        }
        log.error("系统异常", e);
        return Result.error(500, "系统繁忙，请稍后重试");
    }
}




