package com.sinri.passover;

import com.sinri.passover.VertxHttp.AbstractRequestFilter;
import com.sinri.passover.VertxHttp.BasePassoverRouter;
import com.sinri.passover.VertxHttp.VertxHttpGateway;

import java.util.ArrayList;

public class Passover {
    public static void main(String[] args) {
        ArrayList<Class<AbstractRequestFilter>> filterClassList = new ArrayList<>();
        // you can add filters to it

        new VertxHttpGateway()
                .setWorkerPoolSize(10)
                .setLocalListenPort(8000)
                .setFilters(filterClassList)
                .setRouterClass(BasePassoverRouter.class) // you can use your own router implementation
                .run();
    }
}
