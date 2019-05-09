package io.github.sinri.passover.gateway;

import io.github.sinri.passover.gateway.config.ConfigManager;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.LoggerFactory;

/**
 * 网关的核心工作框架，VERTX的承载架子
 */
public class VertxHttpGateway {
    private static Vertx vertx;
    private static ConfigManager configManager;
    private BasePassoverRouter router;

    public VertxHttpGateway() {
        router = configManager.getPassoverConfig().getRouter();
    }

    public static ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * 此方法应该先于VertxHttpGateway实例构造器执行，
     * 以初始化配置管理器。
     *
     * @param configManager the universal config manager
     */
    public static void initializeVertx(ConfigManager configManager) {
        VertxHttpGateway.configManager = configManager;
        vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(configManager.getPassoverConfig().getWorkerPoolSize()));

        LoggerFactory.getLogger(VertxHttpGateway.class).info("initializeVertx done");
    }

    public static Vertx getVertx() {
        return vertx;
    }

    /**
     * 网关的标准运行入口，以配置选项执行网关
     */
    public void run() {
        // 建立网关服务器
        HttpServerOptions options = new HttpServerOptions().setLogActivity(true);
        HttpServer gatewayServer = getVertx().createHttpServer(options);
        // 如果网关服务器出现异常，则进行处理
        gatewayServer.exceptionHandler(exception -> {
            LoggerFactory.getLogger(this.getClass()).info("网关HTTP服务出现异常", exception);
        });

        // 网关服务器处理请求
        gatewayServer.requestHandler(request -> {
            // 创建网关请求封装类，根据路由设置或者判断filters数量，检查是否需要filters
            try {
                new GatewayRequest(request, router).filterAndProxy();
            } catch (Exception e) {
                //e.printStackTrace();
                LoggerFactory.getLogger(this.getClass()).error("大势已去。" + e.getMessage());
            }
        }).listen(configManager.getPassoverConfig().getLocalListenPort());

        LoggerFactory.getLogger(this.getClass()).info("新的网关HTTP服务已经站立在服务器上。" + configManager.getPassoverConfig().toString());

    }
}
