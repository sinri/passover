package com.sinri.passover.VertxHttp;

import com.sinri.passover.VertxHttp.WebExt.CookieExt;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GatewayRequest {
    private Vertx vertx;
    private HttpServerRequest request;
    private String requestId;
    private Buffer bodyBuffer;
    private PassoverRoute route;
    private BasePassoverRouter router;
    //private ArrayList<Class<AbstractRequestFilter>> filters;
    private Logger logger;
    private Map<String, Object> filterShareDataMap;
    private CookieExt cookieExt;

    GatewayRequest(HttpServerRequest request, BasePassoverRouter router, Vertx vertx) {
        this.vertx = vertx;

        this.request = request;
        this.requestId = UUID.randomUUID().toString();

        this.logger = LoggerFactory.getLogger("GR#" + this.requestId);

        // 初始化请求的本体
        this.bodyBuffer = Buffer.buffer();

        // 设定请求出现问题时的回调。此处直接关闭请求。
        request.exceptionHandler(exception -> {
            logger.error("网关请求中出现异常", exception);
            abandonIncomingRequest(AbandonReason.AbandonByIncomingRequestError(exception));
        });

        // 初始化路由
        this.router = router;
        computeRoute();

        // Filters 数据共享初始化
        this.filterShareDataMap = new HashMap<>();

        // 解析Cookie
        this.cookieExt = null;
        request.headers().forEach(header -> {
            logger.debug("Skim Request header item named " + header.getKey() + " : " + header.getValue());
            if (header.getKey().equalsIgnoreCase("cookie")) {
                this.cookieExt = new CookieExt(header.getValue());
                logger.info("Updated CookieExt with item named " + header.getKey());
            }
        });
        if (this.cookieExt == null) {
            this.cookieExt = new CookieExt();
        }
        debugListCookies();
    }

    public CookieExt getCookieExt() {
        return cookieExt;
    }

    public String version() {
        return "1.0-dev";
    }

    public Map<String, Object> getFilterShareDataMap() {
        return filterShareDataMap;
    }

    public Logger getLogger() {
        return logger;
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

    void abandonIncomingRequest(AbandonReason reason) {
        // 是否需要像SLB一样设置一个特殊的报错回复报文，比现在直接关闭更友好一些。
        request.response()
                .setStatusCode(reason.code)
                .setStatusMessage(reason.message)
                .end();
        request.connection().close();
    }

    private void computeRoute() {
        // 根据设定的路由插件计算路由
        logger.info("网关原始请求中的关键字段: " + request.host() + " " + request.remoteAddress().port() + " " + request.isSSL() + " " + request.uri());
        route = router.analyze(request);
        logger.info("计算出的PassoverRoute: " + route);
    }

    void filterAndProxy() {
        // 如果路由显示此请求应该直接废弃，则扔
        if (route.isShouldBeAbandoned()) {
            abandonIncomingRequest(AbandonReason.AbandonByRoute());
            return;
        }
        // 根据路由检查是否需要filters
        if (route.isShouldBeFiltered()) {
            if (!route.isShouldFilterWithBody()) {
                logger.info("开始执行Filters，根据设定，无需Body");
                // 执行Filters
                try {
                    if (applyFilters()) {
                        logger.info("已通过全部Filters无Body校验，开始转发到服务端");
                    } else {
                        return;
                    }
                } catch (Exception applyFilterException) {
                    logger.error("在Filters中出现异常", applyFilterException);
                    abandonIncomingRequest(AbandonReason.AbandonByFilter(applyFilterException));
                    return;
                }
                // Filters全部通过之后就可以直接proxy了
                proxyRequestWithoutFullBody();
            } else {
                // 囤积请求本体
                request.handler(buffer -> {
                    logger.info("囤积Body数据到Buffer，size: " + buffer.length());
                    bodyBuffer.appendBuffer(buffer);
                });

                // 按照文档，这是全部都完成了的回调
                // The endHandler of the request is invoked when the entire request, including any body has been fully read.
                request.endHandler(event -> {
                    logger.info("网关请求Body囤积完毕，开始执行Filters");

                    // 执行Filters
                    try {
                        if (applyFilters()) {
                            logger.info("已通过全部Filters含Body校验，开始转发到服务端");
                        } else {
                            return;
                        }
                    } catch (Exception applyFilterException) {
                        logger.error("在Filters中出现异常", applyFilterException);
                        abandonIncomingRequest(AbandonReason.AbandonByFilter(applyFilterException));
                        return;
                    }
                    proxyRequestWithFullBody();
                });
            }
        } else {
            // 不需要经过filters直接转发
            logger.info("根据设定不需要Filtering，直接开始转发到服务端");
            // 直接proxy
            proxyRequestWithoutFullBody();
        }
    }

    /**
     * Support both kinds of filter, with or without body
     * @return 如果一切正常需要继续转发则返回true，如果执行了Filter定义的回调并停止转发则返回false
     * @throws Exception 如果出现了不可控的异常则扔异常去被abandoned
     */
    private boolean applyFilters() throws Exception {
        // 如果有Filters那就一个个过，找不到filter或者filter失败的话就会抛出异常
        ArrayList<Class<? extends AbstractRequestFilter>> filterClassList = route.getFilterClasses();
        if (filterClassList != null) {
            for (int i = 0; i < filterClassList.size(); i++) {
                Class<? extends AbstractRequestFilter> filterClass = filterClassList.get(i);

                logger.info("第" + i + "个Filter " + filterClass + " 到达门口");

                AbstractRequestFilter requestFilter = filterClass.getDeclaredConstructor(GatewayRequest.class).newInstance(this);
                if (!requestFilter.filter(bodyBuffer)) {
                    logger.error("第" + i + "个Filter " + filterClass + " 未发现门框和门楣上有血，已进门击杀，准备走人。 Feedback: " + requestFilter.getFeedback());
                    return false;
                }
            }
        }

        return true;
    }

    private HttpClientRequest createRequestToService() {
        // 准备转发器并设置连接回调
        HttpClient client = vertx.createHttpClient();
        client.connectionHandler(httpConnection -> logger.info("转发器已连接服务端 " + httpConnection.remoteAddress()));

        // 根据路由准备转发请求的配置
        RequestOptions requestOptions = new RequestOptions()
                .setHost(route.getHost())
                .setPort(route.getPort())
                .setSsl(route.isUseSSL())
                .setURI(route.getUri());

        // 创建转发请求
        HttpClientRequest requestToService = client.request(request.method(), requestOptions, response -> {
            logger.info("转发器收到服务端报文 " + response.statusCode() + " " + response.statusMessage());
            request.response()
                    .setStatusCode(response.statusCode())
                    .setStatusMessage(response.statusMessage());
            StringBuilder headersForLog = new StringBuilder();
            response.headers().forEach(pair -> {
                headersForLog.append(pair.getKey()).append(" : ").append(pair.getValue()).append("\n");
                request.response().putHeader(pair.getKey(), pair.getValue());
            });
            logger.info("转发器收到服务器报文Headers如下\n" + headersForLog);
            Pump.pump(response, request.response()).start();
            response.endHandler(pumpEnd -> {
                logger.info("转发器转发服务端报文完毕，同时结束网关请求");
                request.response().end();
            });

        });
        // 转发请求不需要跟踪30x转移指令
        requestToService.setFollowRedirects(false);
        // 如果转发请求出错，直接关闭请求
        requestToService.exceptionHandler(exception -> {
            logger.error("转发器出现异常将关闭，同时将对网关请求进行报错回复", exception);
            if (requestToService.connection() != null) requestToService.connection().close();
            abandonIncomingRequest(AbandonReason.AbandonByProxy(exception));
        });

        // 为转发请求复刻headers
        StringBuilder headersForLog = new StringBuilder();
        request.headers().forEach(pair -> {
            headersForLog.append(pair.getKey()).append(" : ").append(pair.getValue()).append("\n");
            requestToService.putHeader(pair.getKey(), pair.getValue());
        });
        // 添加Passover的身份字段
        requestToService.putHeader("X-Passover-Version", version());
        headersForLog.append("X-Passover-Version").append(" : ").append(version()).append("\n");
        requestToService.putHeader("X-Passover-Request-Id", requestId);
        headersForLog.append("X-Passover-Request-Id").append(" : ").append(requestId).append("\n");

        logger.info("转发器收到网关请求的Headers如下\n" + headersForLog);

        return requestToService;
    }

    private void proxyRequestWithFullBody() {
        logger.info("囤积的网关请求数据已转发到服务端，坐等服务端回复");
        createRequestToService().end(bodyBuffer);
    }

    private void proxyRequestWithoutFullBody() {
        HttpClientRequest requestToService = createRequestToService();
        request.handler(buffer -> {
            logger.info("从网关请求读取了" + buffer.length() + "字节Body数据并转发到服务端");
            requestToService.write(buffer);
        });

        request.endHandler(event -> {
            logger.info("网关请求数据已全部转发到服务端，坐等服务端回复");
            requestToService.end();
        });
    }

    private void debugListCookies() {
        StringBuilder debug = new StringBuilder();
        cookieExt.getParsedCookieMap().forEach((key, cookie) -> {
            debug.append(key).append(" : ").append(cookie.toString()).append("\n");
        });
        logger.info("Debug Show Cookies:\n" + debug);
    }
}
