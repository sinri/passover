package com.sinri.passover.VertxHttp;

public class AbandonReason {
    int code;
    String message;

    private AbandonReason(int code, String message) {
        this.code = code;
        this.message = message;
    }

    // 请求出现问题
    public static AbandonReason AbandonByIncomingRequestError(Throwable exception) {
        return new AbandonReason(550, "Passover Gateway Request Error: " + exception.getMessage());
    }

    // 路由显示此请求应该直接废弃
    public static AbandonReason AbandonByRoute() {
        return new AbandonReason(551, "Passover Gateway Route Deny.");
    }

    // 执行Filters显示此请求应该直接废弃
    public static AbandonReason AbandonByFilter(Exception applyFilterException) {
        return new AbandonReason(552, "Passover Gateway Filter Deny: " + applyFilterException.getMessage());
    }

    // 转发请求出错
    public static AbandonReason AbandonByProxy(Throwable proxyException) {
        return new AbandonReason(553, "Passover Gateway Proxy Deny: " + proxyException.getMessage());
    }
}
