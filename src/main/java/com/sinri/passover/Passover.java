package com.sinri.passover;

import com.sinri.passover.VertxHttp.VertxHttpGateway;

public class Passover {
    public static void main(String[] args) {
        new VertxHttpGateway()
                .setWorkerPoolSize(10)
                .setLocalListenPort(80)
                .run();
    }
}
