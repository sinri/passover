package io.github.sinri.passover.gateway.config;

import io.github.sinri.passover.gateway.BasePassoverRouter;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class PassoverConfig {
    protected Integer workerPoolSize;
    protected Integer localListenPort;
    protected String routerClassName;

    public PassoverConfig() {
        workerPoolSize = 50;
        localListenPort = 8000;
    }

    public PassoverConfig(Map<String, Object> map) {
        workerPoolSize = Integer.parseInt((String) map.getOrDefault("workerPoolSize", "50"));
        localListenPort = Integer.parseInt((String) map.getOrDefault("localListenPort", "8000"));
        routerClassName = (String) map.getOrDefault("routerClass", "io.github.sinri.passover.gateway.BasePassoverRouter");
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

    public BasePassoverRouter createRouter() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<? extends BasePassoverRouter> subclass = Class.forName(routerClassName).asSubclass(BasePassoverRouter.class);
        return subclass.getDeclaredConstructor().newInstance();
    }
}
