package io.github.sinri.passover.gateway;

public class AbandonReason {
    int code;
    String message;

    private AbandonReason(int code, String message) {
        this.code = code;
        this.message = message;
    }

    // 请求出现问题
    public static AbandonReason AbandonByIncomingRequestError(Throwable exception) {
        return new AbandonReason(550, "因为网关请求出现了迷之异常，Passover拒绝了服务。 " + exception.getMessage());
    }

    // 路由显示此请求应该直接废弃
    public static AbandonReason AbandonByRoute() {
        return new AbandonReason(551, "因为网关请求查询路由异常，Passover拒绝了服务。");
    }

    // 执行Filters显示此请求应该直接废弃
    public static AbandonReason AbandonByFilter(Exception applyFilterException) {
        return new AbandonReason(552, "因为网关请求被Filter弄死，Passover拒绝了服务。 " + applyFilterException.getMessage());
    }

    // 转发请求出错
    public static AbandonReason AbandonByProxy(Throwable proxyException) {
        return new AbandonReason(553, "因为网关请求在转发中出现异常，Passover拒绝了服务。 " + proxyException.getMessage());
    }
}
