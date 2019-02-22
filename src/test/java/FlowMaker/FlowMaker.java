package FlowMaker;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;

public class FlowMaker extends Thread {
    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            FlowMaker thread = new FlowMaker("Thread" + i);
            thread.run();
        }
    }

    protected String name;

    FlowMaker(String name) {
        //name= UUID.randomUUID().toString();
        this.name = name;
    }

    @Override
    public void run() {
        super.run();

        Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(100));
        HttpClient httpClient = vertx.createHttpClient();

        RequestOptions requestOptions = new RequestOptions()
                .setHost("tianwenlook.leqee.com")
                .setPort(443)
                .setSsl(true)
                .setURI("/ssa.php");
        httpClient.request(HttpMethod.GET, requestOptions, response -> {
            System.out.println(name + " RECEIVED " + response.statusCode() + " : " + response.statusMessage() + " from " + response.request().uri());
//            response.bodyHandler(buffer -> {
//                System.out.println("Content:\n" + buffer);
//            });
            httpClient.close();
            vertx.close();
        })
                .putHeader("cookie", "passover_leqee_aa_token=985AA15508273185c6fbf3690ff2")
//                .putHeader("Content-Type", "application/x-www-form-urlencoded")
//                .putHeader("Content-Length", "" + data.getBytes().length)
//                .write(data)
                .end();
    }
}

/*
for i in {1..10};do echo $i;done;
 */