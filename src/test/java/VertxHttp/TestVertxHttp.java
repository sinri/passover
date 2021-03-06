package VertxHttp;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.*;

public class TestVertxHttp {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(40));

        HttpServerOptions options = new HttpServerOptions().setLogActivity(true);
        HttpServer server = vertx.createHttpServer(options);

        server.exceptionHandler(exception -> {
            System.err.println("Exception with server: " + exception.getMessage());
        });

        server.requestHandler(request -> {
            //request.response().end("Hello world");

            HttpClient client = vertx.createHttpClient();
            client.connectionHandler(httpConnection -> {
                System.out.println("Client Connected " + httpConnection.indicatedServerName());
            });

            System.out.println("before " + request.host() + " " + request.remoteAddress().port() + " " + request.isSSL() + " " + request.uri());

            String[] hostParts = request.host().split(":");
            String host = hostParts[0];
            //String port=hostParts.length>1?hostParts[1]:"80";

            RequestOptions requestOptions = new RequestOptions()
                    .setHost(host)
                    .setPort(request.remoteAddress().port())
                    .setSsl(request.isSSL())
                    .setURI(request.uri());
            HttpClientRequest requestToService = client.request(request.method(), requestOptions, response -> {
                System.out.println("response from service " + response.statusCode() + " " + response.statusMessage());
                request.response()
                        .setStatusCode(response.statusCode())
                        .setStatusMessage(response.statusMessage());
                response.headers().forEach(pair -> {
                    request.response().putHeader(pair.getKey(), pair.getValue());
                });
                response.bodyHandler(buffer -> {
                    request.response().write(buffer);
                });
            }).setFollowRedirects(false);

            request.exceptionHandler(exception -> {
                System.err.println("Exception with income request: " + exception.getMessage());
                request.connection().close();
            });

            requestToService.exceptionHandler(exception -> {
                System.err.println("Exception with outgoing request: " + exception.getMessage());
                if (requestToService.connection() != null) requestToService.connection().close();
                request.connection().close();
            });

            System.out.println("requestToService built");

            request.headers().forEach(pair -> {
                System.out.println("See header " + pair.getKey() + " : " + pair.getValue());
                requestToService.putHeader(pair.getKey(), pair.getValue());
            });

            System.out.println("requestToService headers set");

            if (request.getHeader("Content-Length") == null) {
                System.out.println("requestToService to end");
                requestToService.end();
            } else {
                request.bodyHandler(buffer -> {
                    System.out.println("to write buffer " + buffer.length());
                    requestToService.write(buffer);

                    System.out.println("requestToService to end");
                    requestToService.end();
                });
            }
        }).listen(8000);
    }
}
