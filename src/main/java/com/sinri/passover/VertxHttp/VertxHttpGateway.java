package com.sinri.passover.VertxHttp;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.logging.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class VertxHttpGateway {
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

    public void run() {
        // 建立Vertx实例
        Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(workerPoolSize));
        // 建立网关服务器
        HttpServerOptions options = new HttpServerOptions().setLogActivity(true);
        HttpServer gatewayServer = vertx.createHttpServer(options);
        // 如果网关服务器出现异常，则进行处理
        gatewayServer.exceptionHandler(exception -> {
            LoggerFactory.getLogger(this.getClass()).info("Exception with server", exception);
        });
        // 网关服务器处理请求
        gatewayServer.requestHandler(request -> {
            // 初始化请求的本体
            Buffer bodyBuffer = Buffer.buffer();

            // 设定请求出现问题时的回调。此处直接关闭请求。
            request.exceptionHandler(exception -> {
                LoggerFactory.getLogger(this.getClass()).error("Exception with income request", exception);
                request.connection().close();
            });

            // 准备转发器并设置连接回调
            HttpClient client = vertx.createHttpClient();
            client.connectionHandler(httpConnection -> {
                LoggerFactory.getLogger(this.getClass()).info("Client connection established with " + httpConnection.remoteAddress());
            });

            // 根据设定的路由插件计算路由
            LoggerFactory.getLogger(this.getClass()).info("raw meta " + request.host() + " " + request.remoteAddress().port() + " " + request.isSSL() + " " + request.uri());
            BasePassoverRouter router;
            try {
                router = routerClass.getDeclaredConstructor(HttpServerRequest.class).newInstance(request);
            } catch (Exception e) {
                LoggerFactory.getLogger(this.getClass()).error("Cannot found router class " + routerClass, e);
                router = new BasePassoverRouter(request);
            }
            router.analyze();
            LoggerFactory.getLogger(this.getClass()).info("parsed meta " + router.getHost() + " " + router.getPort() + " " + router.isSSL() + " " + router.getUri());

            // 根据路由准备转发请求的配置
            RequestOptions requestOptions = new RequestOptions()
                    .setHost(router.getHost())
                    .setPort(router.getPort())
                    .setSsl(router.isSSL())
                    .setURI(router.getUri());

            // 创建转发请求
            HttpClientRequest requestToService = client.request(request.method(), requestOptions, response -> {
                LoggerFactory.getLogger(this.getClass()).info("response from service " + response.statusCode() + " " + response.statusMessage());
                request.response()
                        .setStatusCode(response.statusCode())
                        .setStatusMessage(response.statusMessage());
                response.headers().forEach(pair -> {
                    request.response().putHeader(pair.getKey(), pair.getValue());
                });
                request.response().headersEndHandler(event -> {
                    LoggerFactory.getLogger(this.getClass()).info("headersEndHandler executing");
                });
                response.bodyHandler(buffer -> {
                    LoggerFactory.getLogger(this.getClass()).info("Body received from service and sent to client " + buffer.length());
                    //request.response().write(buffer);
                    request.response().end(buffer);
                });

            });
            // 转发请求不需要跟踪30x转移指令
            requestToService.setFollowRedirects(false);
            // 如果转发请求出错，直接关闭请求
            requestToService.exceptionHandler(exception -> {
                LoggerFactory.getLogger(this.getClass()).error("Exception with outgoing request", exception);
                if (requestToService.connection() != null) requestToService.connection().close();
                // TODO 是否需要像SLB一样设置一个特殊的报错回复报文，比现在直接关闭更友好一些。
                request.connection().close();
            });

            //LoggerFactory.getLogger(this.getClass()).debug("requestToService built");

            // 为转发请求复刻headers
            request.headers().forEach(pair -> {
                LoggerFactory.getLogger(this.getClass()).info("See header " + pair.getKey() + " : " + pair.getValue());
                requestToService.putHeader(pair.getKey(), pair.getValue());
            });

            //LoggerFactory.getLogger(this.getClass()).debug("requestToService headers set");

            //if (request.getHeader("Content-Length") != null) // 我怀疑不必关心 Content-Length
                request.handler(buffer -> {
                    LoggerFactory.getLogger(this.getClass()).info("I have received a chunk of the body of length " + buffer.length());
                    bodyBuffer.appendBuffer(buffer);
                });

            // 按照文档，这是全部都完成了的回调
            // The endHandler of the request is invoked when the entire request, including any body has been fully read.
            request.endHandler(event -> {
                LoggerFactory.getLogger(this.getClass()).info("income request fully got, requestToService to end");

                // 如果有Filters那就一个个过
                if (filters != null) {
                    for (int i = 0; i < filters.size(); i++) {
                        Class<AbstractRequestFilter> filterClass = filters.get(i);

                        LoggerFactory.getLogger(this.getClass()).info("Filter[" + i + "]" + filterClass + " ready");

                        try {
                            AbstractRequestFilter requestFilter = filterClass.getDeclaredConstructor(HttpServerRequest.class).newInstance(request);
                            if (!requestFilter.filter()) {
                                LoggerFactory.getLogger(this.getClass()).error("Filter[" + i + "]" + filterClass + " denied the request. Feedback: " + requestFilter.getFeedback());
                                request.connection().close();
                                return;
                            }
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            LoggerFactory.getLogger(this.getClass()).error("Cannot make up Filter[" + i + "]" + filterClass + ", request would be denied.", e);
                            request.connection().close();
                            return;
                        }
                    }
                }

                LoggerFactory.getLogger(this.getClass()).info("Filters All Done, ready to send request to service");

                requestToService.end(bodyBuffer);
            });

        }).listen(localListenPort);

        LoggerFactory.getLogger(this.getClass()).info("Main Listen Done on " + localListenPort + " with " + workerPoolSize + " workers");
    }
}
