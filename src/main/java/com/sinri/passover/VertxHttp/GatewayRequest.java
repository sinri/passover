package com.sinri.passover.VertxHttp;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.Pump;

import java.util.ArrayList;
import java.util.UUID;

public class GatewayRequest {
    private Vertx vertx;
    private HttpServerRequest request;
    private String requestId;
    private Buffer bodyBuffer;
    private PassoverRoute route;
    private BasePassoverRouter router;
    private ArrayList<Class<AbstractRequestFilter>> filters;
    private Logger logger;

    GatewayRequest(HttpServerRequest request, BasePassoverRouter router, ArrayList<Class<AbstractRequestFilter>> filters, Vertx vertx) {
        this.vertx = vertx;

        this.request = request;
        this.requestId = UUID.randomUUID().toString();

        this.logger = LoggerFactory.getLogger("GR#" + this.requestId);

        // 初始化请求的本体
        this.bodyBuffer = Buffer.buffer();

        // 设定请求出现问题时的回调。此处直接关闭请求。
        request.exceptionHandler(exception -> {
            logger.error("Exception with income request", exception);
            abandonIncomingRequest(AbandonReason.AbandonByIncomingRequestError(exception));
        });

        // 初始化路由
        this.router = router;
        computeRoute();

        // 登记Filters
        this.filters = filters;
    }

    public PassoverRoute getRoute() {
        return route;
    }

    public String getRequestId() {
        return requestId;
    }

    public Buffer getBodyBuffer() {
        return bodyBuffer;
    }

    public HttpServerRequest getRequest() {
        return request;
    }

    private void abandonIncomingRequest(AbandonReason reason) {
        // 是否需要像SLB一样设置一个特殊的报错回复报文，比现在直接关闭更友好一些。
        request.response()
                .setStatusCode(reason.code)
                .setStatusMessage(reason.message)
                .end();
        request.connection().close();
    }

    private void computeRoute() {
        // 根据设定的路由插件计算路由
        logger.info("raw meta " + request.host() + " " + request.remoteAddress().port() + " " + request.isSSL() + " " + request.uri());
        route = router.analyze(request);
        logger.info("PassoverRoute: " + route);
    }

    void filterAndProxy() {
        // 如果路由显示此请求应该直接废弃，则扔
        if (route.isShouldBeAbandoned()) {
            abandonIncomingRequest(AbandonReason.AbandonByRoute());
            return;
        }
        // 根据路由设置或者判断filters数量，检查是否需要filters
        if (route.isShouldBeFiltered() && filters != null && !filters.isEmpty()) {
            if (!route.isShouldFilterWithBody()) {
                // 执行Filters
                try {
                    // applyFiltersWithoutBody(request);
                    applyFilters();
                } catch (Exception applyFilterException) {
                    logger.error("Error in Filters", applyFilterException);
                    abandonIncomingRequest(AbandonReason.AbandonByFilter(applyFilterException));
                    return;
                }

                logger.info("Filters All Done without body, ready to send request to service");

                // Filters全部通过之后就可以直接proxy了
                proxyRequestWithoutFullBody();
            } else {
                // 囤积请求本体
                request.handler(buffer -> {
                    logger.info("Received a chunk of the body of length " + buffer.length() + " into buffer");
                    bodyBuffer.appendBuffer(buffer);
                });

                // 按照文档，这是全部都完成了的回调
                // The endHandler of the request is invoked when the entire request, including any body has been fully read.
                request.endHandler(event -> {
                    logger.info("income request fully got, requestToService to end");

                    // 执行Filters
                    try {
                        applyFilters();
                    } catch (Exception applyFilterException) {
                        logger.error("Error in Filters", applyFilterException);
                        abandonIncomingRequest(AbandonReason.AbandonByFilter(applyFilterException));
                        return;
                    }

                    logger.info("Filters All Done, ready to send request to service");

                    proxyRequestWithFullBody();
                });
            }
        } else {
            // 不需要经过filters直接转发
            logger.info("Filter-Free, just proxy");
            // 直接proxy
            proxyRequestWithoutFullBody();
        }
    }

    /**
     * Support both kinds of filter, with or without body
     */
    private void applyFilters() throws Exception {
        // 如果有Filters那就一个个过，找不到filter或者filter失败的话就会抛出异常
        if (filters != null) {
            for (int i = 0; i < filters.size(); i++) {
                Class<AbstractRequestFilter> filterClass = filters.get(i);

                logger.info("Filter[" + i + "]" + filterClass + " ready");

                AbstractRequestFilter requestFilter = filterClass.getDeclaredConstructor(HttpServerRequest.class).newInstance(request);
                if (!requestFilter.filter(bodyBuffer)) {
                    logger.error("Filter[" + i + "]" + filterClass + " denied the request. Feedback: " + requestFilter.getFeedback());
                    throw new Exception("Filter[" + i + "]" + filterClass + " denied the request. Feedback: " + requestFilter.getFeedback());
                }
            }
        }

        logger.info("Filters All Done, ready to send request to service");
    }

    private HttpClientRequest createRequestToService() {
        // 准备转发器并设置连接回调
        HttpClient client = vertx.createHttpClient();
        client.connectionHandler(httpConnection -> logger.info("Client connection established with " + httpConnection.remoteAddress()));

        // 根据路由准备转发请求的配置
        RequestOptions requestOptions = new RequestOptions()
                .setHost(route.getHost())
                .setPort(route.getPort())
                .setSsl(route.isUseSSL())
                .setURI(route.getUri());

        // 创建转发请求
        HttpClientRequest requestToService = client.request(request.method(), requestOptions, response -> {
            logger.info("response from service " + response.statusCode() + " " + response.statusMessage());
            request.response()
                    .setStatusCode(response.statusCode())
                    .setStatusMessage(response.statusMessage());
            response.headers().forEach(pair -> {
                logger.info("Service Response Header " + pair.getKey() + " : " + pair.getValue());
                request.response().putHeader(pair.getKey(), pair.getValue());
            });
            //request.response().headersEndHandler(event2 -> logger.info("headersEndHandler executing"));
//            response.bodyHandler(buffer -> {
//                logger.info("All body received from service and sent to client " + buffer.length());
//                //request.response().write(buffer);
//                request.response().end(buffer);
//            });

            Pump.pump(response, request.response()).start();
            response.endHandler(pumpEnd -> {
                logger.info("Response Body Pumped Back To Client, End Gateway Request");
                request.response().end();
            });

        });
        // 转发请求不需要跟踪30x转移指令
        requestToService.setFollowRedirects(false);
        // 如果转发请求出错，直接关闭请求
        requestToService.exceptionHandler(exception -> {
            logger.error("Exception with outgoing request", exception);
            if (requestToService.connection() != null) requestToService.connection().close();
            abandonIncomingRequest(AbandonReason.AbandonByProxy(exception));
        });

        //logger.debug("requestToService built");

        // 为转发请求复刻headers
        request.headers().forEach(pair -> {
            logger.info("See header " + pair.getKey() + " : " + pair.getValue());
            requestToService.putHeader(pair.getKey(), pair.getValue());
        });

        return requestToService;
    }

    private void proxyRequestWithFullBody() {
        createRequestToService().end(bodyBuffer);
    }

    private void proxyRequestWithoutFullBody() {
        HttpClientRequest requestToService = createRequestToService();

        request.handler(buffer -> {
            logger.info("Received a chunk of the body of length " + buffer.length() + " and directly proxied");
            requestToService.write(buffer);
        });

        request.endHandler(event -> {
            logger.info("income request fully got, requestToService to end");
            requestToService.end();
        });
    }
}
