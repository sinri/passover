package io.github.sinri.passover;

import io.github.sinri.passover.gateway.VertxHttpGateway;
import io.github.sinri.passover.sample.Router.FirstRouter;

public class Passover {
    public static void main(String[] args) {
        new VertxHttpGateway()
                .setWorkerPoolSize(10)
                .setLocalListenPort(8000)
                .setRouter(new FirstRouter()) // you can use your own router implementation
                .run();
    }
}
