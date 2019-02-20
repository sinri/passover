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
        Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(workerPoolSize));

        HttpServerOptions options = new HttpServerOptions().setLogActivity(true);
        HttpServer server = vertx.createHttpServer(options);

        server.exceptionHandler(exception -> {
            LoggerFactory.getLogger(this.getClass()).info("Exception with server", exception);
        });

        server.requestHandler(request -> {
            Buffer bodyBuffer = Buffer.buffer();

            HttpClient client = vertx.createHttpClient();
            client.connectionHandler(httpConnection -> {
                LoggerFactory.getLogger(this.getClass()).info("Client connection established with " + httpConnection.remoteAddress());
            });

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

            RequestOptions requestOptions = new RequestOptions()
                    .setHost(router.getHost())
                    .setPort(router.getPort())
                    .setSsl(router.isSSL())
                    .setURI(router.getUri());
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

            }).setFollowRedirects(false);

            request.exceptionHandler(exception -> {
                LoggerFactory.getLogger(this.getClass()).error("Exception with income request", exception);
                request.connection().close();
            });

            requestToService.exceptionHandler(exception -> {
                LoggerFactory.getLogger(this.getClass()).error("Exception with outgoing request", exception);
                if (requestToService.connection() != null) requestToService.connection().close();
                request.connection().close();
            });

            LoggerFactory.getLogger(this.getClass()).info("requestToService built");

            request.headers().forEach(pair -> {
                LoggerFactory.getLogger(this.getClass()).info("See header " + pair.getKey() + " : " + pair.getValue());
                requestToService.putHeader(pair.getKey(), pair.getValue());
            });

            LoggerFactory.getLogger(this.getClass()).info("requestToService headers set");

            if (request.getHeader("Content-Length") == null) {
                //LoggerFactory.getLogger(this.getClass()).info("requestToService to end");
                //requestToService.end();
            } else {
//                request.bodyHandler(buffer -> {
//                    LoggerFactory.getLogger(this.getClass()).info("to write buffer " + buffer.length());
//                    requestToService.write(buffer);
//
//                    //LoggerFactory.getLogger(this.getClass()).info("requestToService to end");
//                    //requestToService.end();
//                });

                request.handler(buffer -> {
                    System.out.println("I have received a chunk of the body of length " + buffer.length());
                    bodyBuffer.appendBuffer(buffer);
                });
            }

            request.endHandler(event -> {
                LoggerFactory.getLogger(this.getClass()).info("income request fully got, requestToService to end");

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
