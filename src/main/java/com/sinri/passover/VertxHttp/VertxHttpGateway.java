package com.sinri.passover.VertxHttp;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;

public class VertxHttpGateway {
    private Vertx vertx;
    private int workerPoolSize = 40;
    private int localListenPort = 80;
    private Class<BasePassoverRouter> routerClass = BasePassoverRouter.class;
    private ArrayList<Class<AbstractRequestFilter>> filters = new ArrayList<>();

    public Class<BasePassoverRouter> getRouterClass() {
        return routerClass;
    }

    public VertxHttpGateway setRouterClass(Class<BasePassoverRouter> routerClass) {
        this.routerClass = routerClass;
        return this;
    }

    public ArrayList<Class<AbstractRequestFilter>> getFilters() {
        return filters;
    }

    public VertxHttpGateway setFilters(ArrayList<Class<AbstractRequestFilter>> filters) {
        this.filters = filters;
        return this;
    }

    public int getWorkerPoolSize() {
        return workerPoolSize;
    }

    public VertxHttpGateway setWorkerPoolSize(int workerPoolSize) {
        this.workerPoolSize = workerPoolSize;
        return this;
    }

    public int getLocalListenPort() {
        return localListenPort;
    }

    public VertxHttpGateway setLocalListenPort(int localListenPort) {
        this.localListenPort = localListenPort;
        return this;
    }

    private BasePassoverRouter buildRouter() {
        try {
            return routerClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass()).error("Cannot found router class " + routerClass, e);
        }
        return new BasePassoverRouter();
    }

    public void run() {
        // 建立Vertx实例
        vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(workerPoolSize));
        // 建立网关服务器
        HttpServerOptions options = new HttpServerOptions().setLogActivity(true);
        HttpServer gatewayServer = vertx.createHttpServer(options);
        // 如果网关服务器出现异常，则进行处理
        gatewayServer.exceptionHandler(exception -> {
            LoggerFactory.getLogger(this.getClass()).info("Exception with the gateway server", exception);
        });

        // 初始化路由器
        final BasePassoverRouter router = buildRouter();

        // 网关服务器处理请求
        gatewayServer.requestHandler(request -> {
            // 创建网关请求封装类，根据路由设置或者判断filters数量，检查是否需要filters
            new GatewayRequest(request, router, filters, vertx).filterAndProxy();
        }).listen(localListenPort);

        LoggerFactory.getLogger(this.getClass()).info("Main Listen Done on " + localListenPort + " with " + workerPoolSize + " workers");

    }
}
