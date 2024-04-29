package com.yu.gateway.common.enums;

import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;

/**
 * @author yu
 * @description 请求响应编码
 * @date 2024-03-31
 */
@Getter
public enum ResponseCode {

    SUCCESS(HttpResponseStatus.OK, 0, "成功"),
    UNAUTHORIZED(HttpResponseStatus.UNAUTHORIZED, 401, "用户未登陆"),
    INTERNAL_ERROR(HttpResponseStatus.INTERNAL_SERVER_ERROR, 1000, "网关内部错误"),
    SERVICE_UNAVAILABLE(HttpResponseStatus.SERVICE_UNAVAILABLE, 2000, "服务暂时不可用,请稍后再试"),

    REQUEST_PARSE_ERROR(HttpResponseStatus.BAD_REQUEST, 10000, "请求解析错误, header中必须存在uniqueId参数"),
    REQUEST_PARSE_ERROR_NO_UNIQUEID(HttpResponseStatus.BAD_REQUEST, 10001, "请求解析错误, header中必须存在uniqueId参数"),
    PATH_NO_MATCHED(HttpResponseStatus.NOT_FOUND,10002, "没有找到匹配的路径, 请求快速失败"),
    SERVICE_DEFINITION_NOT_FOUND(HttpResponseStatus.NOT_FOUND,10003, "未找到对应的服务定义"),
    SERVICE_INVOKER_NOT_FOUND(HttpResponseStatus.NOT_FOUND,10004, "未找到对应的调用实例"),
    SERVICE_INSTANCE_NOT_FOUND(HttpResponseStatus.NOT_FOUND,10005, "未找到对应的服务实例"),
    FILTER_CONFIG_PARSE_ERROR(HttpResponseStatus.INTERNAL_SERVER_ERROR,10006, "过滤器配置解析异常"),

    REQUEST_TIMEOUT(HttpResponseStatus.GATEWAY_TIMEOUT, 10007, "连接下游服务超时"),

    HTTP_RESPONSE_ERROR(HttpResponseStatus.INTERNAL_SERVER_ERROR, 10030, "服务返回异常"),

    USERID_MISMATCH(HttpResponseStatus.BAD_REQUEST, 10001, "请求参数错误"),
    VERIFICATION_FAILED(HttpResponseStatus.BAD_REQUEST,10030, "请求参数校验失败"),
    BLACKLIST(HttpResponseStatus.FORBIDDEN,10004, "请求IP在黑名单"),
    WHITELIST(HttpResponseStatus.FORBIDDEN,10005, "请求IP不在白名单"),

    GATEWAY_FALLBACK_TIMEOUT(HttpResponseStatus.GATEWAY_TIMEOUT, 10055, "请求超时, cause hystrix fallback"),
    GATEWAY_FALLBACK_ERROR(HttpResponseStatus.INTERNAL_SERVER_ERROR, 10066, "服务器内部错误, 导致 hystrix fallback");

    private HttpResponseStatus status;
    private int code;
    private String message;

    ResponseCode(HttpResponseStatus status, int code, String msg) {
        this.status = status;
        this.code = code;
        this.message = msg;
    }
}
