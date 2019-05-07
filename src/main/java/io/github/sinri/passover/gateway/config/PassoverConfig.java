package io.github.sinri.passover.gateway.config;

import io.github.sinri.passover.gateway.BasePassoverRouter;
import io.vertx.core.logging.LoggerFactory;

import java.util.Map;

public class PassoverConfig {
    protected Integer workerPoolSize;
    protected Integer localListenPort;
    protected String routerClassName;
    private BasePassoverRouter router;

    public PassoverConfig() {
        workerPoolSize = 50;
        localListenPort = 8000;
        routerClassName = "io.github.sinri.passover.gateway.BasePassoverRouter";
        makeRouter();
    }

    public PassoverConfig(Map<String, Object> map) {
        workerPoolSize = (Integer) map.getOrDefault("workerPoolSize", "50");
        localListenPort = (Integer) map.getOrDefault("localListenPort", "8000");
        routerClassName = (String) map.getOrDefault("routerClass", "io.github.sinri.passover.gateway.BasePassoverRouter");
        makeRouter();
    }

    private void makeRouter() {
        try {
            Class<? extends BasePassoverRouter> subclass = Class.forName(routerClassName).asSubclass(BasePassoverRouter.class);
            router = subclass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass()).warn("无法加载给定的Router类，使用默认Router", e);
            router = new BasePassoverRouter();
        }
    }

    public String getRouterClassName() {
        return routerClassName;
    }

    public void setRouterClassName(String routerClassName) {
        this.routerClassName = routerClassName;
    }

    public Integer getLocalListenPort() {
        return localListenPort;
    }

    public void setLocalListenPort(Integer localListenPort) {
        this.localListenPort = localListenPort;
    }

    public Integer getWorkerPoolSize() {
        return workerPoolSize;
    }

    public void setWorkerPoolSize(Integer workerPoolSize) {
        this.workerPoolSize = workerPoolSize;
    }

    public BasePassoverRouter getRouter() {
        return router;
    }

    @Override
    public String toString() {
        return "监听端口: " + localListenPort + " 线程数量: " + workerPoolSize + " Router: " + router.name();
    }
}
