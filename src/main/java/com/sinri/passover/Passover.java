package com.sinri.passover;

import com.sinri.passover.VertxHttp.Router.FirstRouter;
import com.sinri.passover.VertxHttp.VertxHttpGateway;

public class Passover {
    public static void main(String[] args) {
        new VertxHttpGateway()
                .setWorkerPoolSize(10)
                .setLocalListenPort(8000)
                .setRouter(new FirstRouter()) // you can use your own router implementation
                .run();
    }
}
