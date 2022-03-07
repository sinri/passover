package io.github.sinri.passover.router;

import io.github.sinri.keel.Keel;
import io.github.sinri.keel.core.logger.KeelLogger;
import io.github.sinri.keel.core.properties.KeelOptions;
import io.github.sinri.passover.Passover;
import io.github.sinri.passover.core.RouteOptions;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.RequestOptions;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class PassoverRoute extends KeelOptions {
    // RELAY or ABANDON
    private final static String METHOD_RELAY = "RELAY";
    private final static String METHOD_ABANDON = "ABANDON";

    private final RouteOptions routeOptions;
    private final RouteOptions.MethodRelayOptions relayOptions;
    private final KeelLogger logger;

    private final AtomicReference<HttpServerRequest> atomicHttpServerRequest = new AtomicReference<>();
    private final AtomicReference<Buffer> atomicBody = new AtomicReference<>();

    public PassoverRoute(RouteOptions routeOptions) {
        this.routeOptions = routeOptions;
        this.relayOptions = routeOptions.getRelay();
        String requestId = UUID.randomUUID().toString();
        logger = Keel.standaloneLogger("request").setCategoryPrefix(requestId);
    }

    public static PassoverRoute findMappedRoute(HttpServerRequest httpServerRequest) {
        Keel.logger("Passover").info("findMappedRoute: " + httpServerRequest.host() + " and " + httpServerRequest.path());

        String host = httpServerRequest.host();
        String[] split = httpServerRequest.host().split(":");
        if (split.length > 0) {
            host = split[0];
        }
        for (var route : Passover.getPassoverOptions().getRoutes()) {
            if (!route.isApplicableForRequest(host, httpServerRequest.path())) {
                continue;
            }
            return new PassoverRoute(route);
        }
        return null;
    }

    public String getRouteName() {
        return routeOptions.getRouteName();
    }

    public void handleRequest(HttpServerRequest httpServerRequest) {
        atomicHttpServerRequest.set(httpServerRequest);

        if (METHOD_RELAY.equals(routeOptions.getMethod())) {
            if (isBodyNeeded() && relayOptions.isFiltersUseBody()) {
                logger.info("isFiltersUseBody: YES 准备读取body");
                httpServerRequest.body()
                        .compose(buffer -> {
                            atomicBody.set(buffer);
                            logger.info("读取到了 body 为 " + buffer.length() + " 字节");
                            relayRequest(httpServerRequest);
                            return Future.succeededFuture();
                        });
            } else {
                relayRequest(httpServerRequest);
            }
        } else if (METHOD_ABANDON.equals(routeOptions.getMethod())) {
            abandonRequest(httpServerRequest);
        } else {
            abandonRequest(httpServerRequest);
        }
    }

    protected void abandonRequest(HttpServerRequest httpServerRequest) {
        httpServerRequest.response().setStatusCode(406).end();
        logger.warning("request abandoned");
    }

    protected void relayRequest(HttpServerRequest httpServerRequest) {
        logger.info("relayRequest 开始");
        for (var filterOption : relayOptions.getFilters()) {
            String filterName = filterOption.getFilterName();
            List<String> filterParams = filterOption.getFilterParams();

            PassoverFilter filter = PassoverFilter.factory(filterName, filterParams, logger);
            if (filter == null) {
                logger.error("Configured Filter " + filterName + " Not Available! Abandon.");
                abandonRequest(httpServerRequest);
                return;
            }

            filter.setBodyBuffer(atomicBody.get());

            boolean passover = filter.passoverOrNot(httpServerRequest);
            if (!passover) {
                logger.warning("Filter " + filterName + ": Do not passover this request");
                return;
            }

            logger.info("Filter " + filterName + ": Passover this request");
        }

        logger.info("All Filters Passover");
        AtomicReference<HttpClientRequest> atomicHttpClientRequest = new AtomicReference<>();
        RequestOptions requestOptions = new RequestOptions()
                .setMethod(httpServerRequest.method())
                .setHost(relayOptions.getHost())
                .setPort(relayOptions.getPort())
                .setSsl(relayOptions.isUseHttps())
                .setURI(httpServerRequest.uri() + "?" + httpServerRequest.query())
                .setHeaders(httpServerRequest.headers());
        Keel.getVertx().createHttpClient()
                .request(requestOptions)
                .compose(httpClientRequest -> {
                    atomicHttpClientRequest.set(httpClientRequest);

                    if (isBodyNeeded()) {
                        if (relayOptions.isFiltersUseBody()) {
                            logger.info("原始请求的body 已经获取过了");
                            return Future.succeededFuture();
                        } else {
                            logger.info("准备获取原始请求的body");
                            return httpServerRequest.body()
                                    .compose(buffer -> {
                                        atomicBody.set(buffer);
                                        logger.info("获取原始请求的body为" + buffer.length() + "字节并准备发送给真实服务");
                                        return Future.succeededFuture();
                                    });
                        }
                    } else {
                        logger.info("不是POST不管body了");
                        return Future.succeededFuture();
                    }
                })
                .compose(v -> {
                    if (isBodyNeeded()) {
                        logger.info("获取原始请求的body为" + atomicBody.get().length() + "字节并准备发送给真实服务");
                        return atomicHttpClientRequest.get().end(atomicBody.get());
                    } else {
                        return atomicHttpClientRequest.get().end();
                    }
                })
                .compose(v -> atomicHttpClientRequest.get().response())
                .compose(httpClientResponse -> {
                    logger.info("Response from actual server: " + httpClientResponse.statusCode() + " " + httpClientResponse.statusMessage());
                    httpServerRequest.response().setStatusCode(httpClientResponse.statusCode());
                    httpClientResponse.headers().forEach(stringStringEntry -> httpServerRequest.response().putHeader(stringStringEntry.getKey(), stringStringEntry.getValue()));
                    return httpClientResponse.body().compose(buffer -> httpServerRequest.response().end(buffer));
                })
                .onFailure(throwable -> {
                    logger.exception("大势已去", throwable);
                })
                .onSuccess(v -> {
                    logger.notice("大事已成");
                });
    }

    private boolean isBodyNeeded() {
        return atomicHttpServerRequest.get().method() == HttpMethod.POST || atomicHttpServerRequest.get().method() == HttpMethod.PUT;
    }
}
