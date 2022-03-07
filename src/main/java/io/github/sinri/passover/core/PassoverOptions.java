package io.github.sinri.passover.core;

import io.github.sinri.keel.core.properties.KeelOptions;

import java.util.ArrayList;
import java.util.List;

public class PassoverOptions extends KeelOptions {
    // options for passover and vertx, with prefix "passover"
    private int workerPoolSize;
    private int localListenPort;
    private String routerClass;
    private String casServiceName;
    private List<RouteOptions> routes;

    public PassoverOptions() {
        this.workerPoolSize = 16;
        this.localListenPort = 8000;
        this.routerClass = "io.github.sinri.passover.gateway.UnknownRouter";
        this.casServiceName = "unknown-passover";
        this.routes = new ArrayList<>();
    }

    public int getWorkerPoolSize() {
        return workerPoolSize;
    }

    public PassoverOptions setWorkerPoolSize(int workerPoolSize) {
        this.workerPoolSize = workerPoolSize;
        return this;
    }

    public int getLocalListenPort() {
        return localListenPort;
    }

    public PassoverOptions setLocalListenPort(int localListenPort) {
        this.localListenPort = localListenPort;
        return this;
    }

    public String getRouterClass() {
        return routerClass;
    }

    public PassoverOptions setRouterClass(String routerClass) {
        this.routerClass = routerClass;
        return this;
    }

    public String getCasServiceName() {
        return casServiceName;
    }

    public PassoverOptions setCasServiceName(String casServiceName) {
        this.casServiceName = casServiceName;
        return this;
    }

    public List<RouteOptions> getRoutes() {
        return routes;
    }

    public PassoverOptions setRoutes(List<RouteOptions> routes) {
        this.routes = routes;
        return this;
    }


}
