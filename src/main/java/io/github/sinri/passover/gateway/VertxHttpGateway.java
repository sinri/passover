package io.github.sinri.passover.gateway;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.LoggerFactory;

public class VertxHttpGateway {
    private static Vertx vertx;
    private static int workerPoolSize = 40;

    static {
        // 建立Vertx实例
        vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(workerPoolSize));
    }

    private int localListenPort = 80;
    private BasePassoverRouter router = new BasePassoverRouter();

    public static Vertx getVertx() {
        return vertx;
    }

    public BasePassoverRouter getRouter() {
        return router;
    }

    public VertxHttpGateway setRouter(BasePassoverRouter router) {
        this.router = router;
        return this;
    }

    public static int getWorkerPoolSize() {
        return workerPoolSize;
    }

    public static void setWorkerPoolSize(int workerPoolSize) {
        VertxHttpGateway.workerPoolSize = workerPoolSize;
    }

    public int getLocalListenPort() {
        return localListenPort;
    }

    public VertxHttpGateway setLocalListenPort(int localListenPort) {
        this.localListenPort = localListenPort;
        return this;
    }

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
            new GatewayRequest(request, router).filterAndProxy();
        }).listen(localListenPort);

        LoggerFactory.getLogger(this.getClass()).info("新的网关HTTP服务已经站立在服务器上，监听" + localListenPort + "端口，线程数量:" + workerPoolSize);

    }
}
