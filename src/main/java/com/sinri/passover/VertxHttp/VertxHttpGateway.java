package com.sinri.passover.VertxHttp;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.LoggerFactory;

public class VertxHttpGateway {
    private Vertx vertx;
    private int workerPoolSize = 40;
    private int localListenPort = 80;
    private Class<BasePassoverRouter> routerClass = BasePassoverRouter.class;
    //private ArrayList<Class<AbstractRequestFilter>> filters = new ArrayList<>();

    public Class<BasePassoverRouter> getRouterClass() {
        return routerClass;
    }

    public VertxHttpGateway setRouterClass(Class<BasePassoverRouter> routerClass) {
        this.routerClass = routerClass;
        return this;
    }

//    public ArrayList<Class<AbstractRequestFilter>> getFilters() {
//        return filters;
//    }

//    public VertxHttpGateway setFilters(ArrayList<Class<AbstractRequestFilter>> filters) {
//        this.filters = filters;
//        return this;
//    }

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
            LoggerFactory.getLogger(this.getClass()).error("无法找到Router类，根据以下类定义: " + routerClass, e);
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
            LoggerFactory.getLogger(this.getClass()).info("网关HTTP服务出现异常", exception);
        });

        // 初始化路由器
        final BasePassoverRouter router = buildRouter();

        // 网关服务器处理请求
        gatewayServer.requestHandler(request -> {
            // 创建网关请求封装类，根据路由设置或者判断filters数量，检查是否需要filters
            new GatewayRequest(request, router, vertx).filterAndProxy();
        }).listen(localListenPort);

        LoggerFactory.getLogger(this.getClass()).info("新的网关HTTP服务已经站立在服务器上，监听" + localListenPort + "端口，线程数量:" + workerPoolSize);

    }
}
