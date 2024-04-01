package com.yu.gateway.core.context;


public enum ContextStatus {
    Running(0, "运行中"),
    Written(1, "请求结束，写回响应"),
    Completed(2, "成功返回响应"),
    Terminated(3, "整体网关请求结束");

    ContextStatus(int status, String description) {
        this.status = status;
        this.description = description;
    }

    public int status;
    public String description;
}
