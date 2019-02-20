package com.sinri.passover.VertxHttp;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.*;
import io.vertx.core.logging.LoggerFactory;

public class VertxHttpGateway {
    private int workerPoolSize = 40;
    private int localListenPort = 80;

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
            HttpClient client = vertx.createHttpClient();
            client.connectionHandler(httpConnection -> {
                LoggerFactory.getLogger(this.getClass()).info("Client connection established with " + httpConnection.remoteAddress());
            });

            LoggerFactory.getLogger(this.getClass()).info("raw meta " + request.host() + " " + request.remoteAddress().port() + " " + request.isSSL() + " " + request.uri());

            String[] hostParts = request.host().split(":");
            String host = hostParts[0];
            int port = request.isSSL() ? 443 : 80;

            LoggerFactory.getLogger(this.getClass()).info("parsed meta " + host + " " + port + " " + request.isSSL() + " " + request.uri());

            RequestOptions requestOptions = new RequestOptions()
                    .setHost(host)
                    .setPort(port)
                    .setSsl(request.isSSL())
                    .setURI(request.uri());
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
                LoggerFactory.getLogger(this.getClass()).info("requestToService to end");
                requestToService.end();
            } else {
                request.bodyHandler(buffer -> {
                    LoggerFactory.getLogger(this.getClass()).info("to write buffer " + buffer.length());
                    requestToService.write(buffer);

                    LoggerFactory.getLogger(this.getClass()).info("requestToService to end");
                    requestToService.end();
                });
            }
        }).listen(localListenPort);

        LoggerFactory.getLogger(this.getClass()).info("Main Listen Done on " + localListenPort + " with " + workerPoolSize + " workers");
    }
}
