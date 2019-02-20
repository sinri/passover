package com.sinri.passover.VertxHttp;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;

public class VertxHttpGateway {
    Vertx vertx;
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
        vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(workerPoolSize));
        // 建立网关服务器
        HttpServerOptions options = new HttpServerOptions().setLogActivity(true);
        HttpServer gatewayServer = vertx.createHttpServer(options);
        // 如果网关服务器出现异常，则进行处理
        gatewayServer.exceptionHandler(exception -> {
            LoggerFactory.getLogger(this.getClass()).info("Exception with the gateway server", exception);
        });

        // 初始化路由器
        BasePassoverRouter nonFinalRouter;
        try {
            nonFinalRouter = routerClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass()).error("Cannot found router class " + routerClass, e);
            nonFinalRouter = new BasePassoverRouter();
        }
        final BasePassoverRouter router = nonFinalRouter;

        // 网关服务器处理请求
        gatewayServer.requestHandler(request -> {
            // 初始化请求的本体
            Buffer bodyBuffer = Buffer.buffer();

            // 设定请求出现问题时的回调。此处直接关闭请求。
            request.exceptionHandler(exception -> {
                LoggerFactory.getLogger(this.getClass()).error("Exception with income request", exception);
                abandonIncomingRequest(request, AbandonReason.AbandonByIncomingRequestError(exception));
            });

            // 根据设定的路由插件计算路由
            LoggerFactory.getLogger(this.getClass()).info("raw meta " + request.host() + " " + request.remoteAddress().port() + " " + request.isSSL() + " " + request.uri());
            PassoverRoute route = router.analyze(request);
            LoggerFactory.getLogger(this.getClass()).info("PassoverRoute: " + route);

            // 如果路由显示此请求应该直接废弃，则扔
            if (route.isShouldBeAbandoned()) {
                abandonIncomingRequest(request, AbandonReason.AbandonByRoute());
                return;
            }

            // 根据路由设置或者判断filters数量，检查是否需要filters
            if (route.isShouldBeFiltered() && filters != null && !filters.isEmpty()) {
                if (!route.isShouldFilterWithBody()) {
                    // 执行Filters
                    try {
                        applyFiltersWithoutBody(request);
                    } catch (Exception applyFilterException) {
                        LoggerFactory.getLogger(this.getClass()).error("Error in Filters", applyFilterException);
                        abandonIncomingRequest(request, AbandonReason.AbandonByFilter(applyFilterException));
                        return;
                    }

                    LoggerFactory.getLogger(this.getClass()).info("Filters All Done without body, ready to send request to service");

                    // Filters全部通过之后就可以直接proxy了
                    proxyRequestWithoutFullBody(request, route);
                } else {
                    // 囤积请求本体
                    request.handler(buffer -> {
                        LoggerFactory.getLogger(this.getClass()).info("Received a chunk of the body of length " + buffer.length() + " into buffer");
                        bodyBuffer.appendBuffer(buffer);
                    });

                    // 按照文档，这是全部都完成了的回调
                    // The endHandler of the request is invoked when the entire request, including any body has been fully read.
                    request.endHandler(event -> {
                        LoggerFactory.getLogger(this.getClass()).info("income request fully got, requestToService to end");

                        // 执行Filters
                        try {
                            applyFilters(request, bodyBuffer);
                        } catch (Exception applyFilterException) {
                            LoggerFactory.getLogger(this.getClass()).error("Error in Filters", applyFilterException);
                            abandonIncomingRequest(request, AbandonReason.AbandonByFilter(applyFilterException));
                            return;
                        }

                        LoggerFactory.getLogger(this.getClass()).info("Filters All Done, ready to send request to service");

                        proxyRequestWithFullBody(request, route, bodyBuffer);
                    });
                }
            } else {
                // 不需要经过filters直接转发
                LoggerFactory.getLogger(this.getClass()).info("Filter-Free, just proxy");
                // 直接proxy
                proxyRequestWithoutFullBody(request, route);
            }
        }).listen(localListenPort);

        LoggerFactory.getLogger(this.getClass()).info("Main Listen Done on " + localListenPort + " with " + workerPoolSize + " workers");

    }

    /**
     * @param request    incoming request
     * @param bodyBuffer incoming body data
     */
    protected void applyFilters(HttpServerRequest request, Buffer bodyBuffer) throws Exception {
        // 如果有Filters那就一个个过，找不到filter或者filter失败的话就会抛出异常
        if (filters != null) {
            for (int i = 0; i < filters.size(); i++) {
                Class<AbstractRequestFilter> filterClass = filters.get(i);

                LoggerFactory.getLogger(this.getClass()).info("Filter[" + i + "]" + filterClass + " ready");

                AbstractRequestFilter requestFilter = filterClass.getDeclaredConstructor(HttpServerRequest.class).newInstance(request);
                if (!requestFilter.filter(bodyBuffer)) {
                    LoggerFactory.getLogger(this.getClass()).error("Filter[" + i + "]" + filterClass + " denied the request. Feedback: " + requestFilter.getFeedback());
                    throw new Exception("Filter[" + i + "]" + filterClass + " denied the request. Feedback: " + requestFilter.getFeedback());
                }
            }
        }

        LoggerFactory.getLogger(this.getClass()).info("Filters All Done, ready to send request to service");
    }

    protected void applyFiltersWithoutBody(HttpServerRequest request) throws Exception {
        applyFilters(request, null);
    }

    protected void abandonIncomingRequest(HttpServerRequest request, AbandonReason reason) {
        // 是否需要像SLB一样设置一个特殊的报错回复报文，比现在直接关闭更友好一些。
        request.response()
                .setStatusCode(reason.code)
                .setStatusMessage(reason.message)
                .end();
        request.connection().close();
    }

    private HttpClientRequest createRequestToService(HttpServerRequest request, PassoverRoute route) {
        // 准备转发器并设置连接回调
        HttpClient client = vertx.createHttpClient();
        client.connectionHandler(httpConnection -> {
            LoggerFactory.getLogger(this.getClass()).info("Client connection established with " + httpConnection.remoteAddress());
        });

        // 根据路由准备转发请求的配置
        RequestOptions requestOptions = new RequestOptions()
                .setHost(route.getHost())
                .setPort(route.getPort())
                .setSsl(route.isUseSSL())
                .setURI(route.getUri());

        // 创建转发请求
        HttpClientRequest requestToService = client.request(request.method(), requestOptions, response -> {
            LoggerFactory.getLogger(this.getClass()).info("response from service " + response.statusCode() + " " + response.statusMessage());
            request.response()
                    .setStatusCode(response.statusCode())
                    .setStatusMessage(response.statusMessage());
            response.headers().forEach(pair -> {
                request.response().putHeader(pair.getKey(), pair.getValue());
            });
            request.response().headersEndHandler(event2 -> {
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
            abandonIncomingRequest(request, AbandonReason.AbandonByProxy(exception));
        });

        //LoggerFactory.getLogger(this.getClass()).debug("requestToService built");

        // 为转发请求复刻headers
        request.headers().forEach(pair -> {
            LoggerFactory.getLogger(this.getClass()).info("See header " + pair.getKey() + " : " + pair.getValue());
            requestToService.putHeader(pair.getKey(), pair.getValue());
        });

        return requestToService;
    }

    protected void proxyRequestWithFullBody(HttpServerRequest request, PassoverRoute route, Buffer bodyBuffer) {
        createRequestToService(request, route).end(bodyBuffer);
    }

    protected void proxyRequestWithoutFullBody(HttpServerRequest request, PassoverRoute route) {
        HttpClientRequest requestToService = createRequestToService(request, route);

        request.handler(buffer -> {
            LoggerFactory.getLogger(this.getClass()).info("Received a chunk of the body of length " + buffer.length() + " and directly proxied");
            requestToService.write(buffer);
        });

        request.endHandler(event -> {
            LoggerFactory.getLogger(this.getClass()).info("income request fully got, requestToService to end");
            requestToService.end();
        });
    }
}
