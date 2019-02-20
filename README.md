# Passover

为鲵搞的简单的网关系统。

这个版本基于Vertx。

原理是用Vert.x启动HttpServer，收到HTTP(S)请求时（处理一波之后）重新包装成一个请求发送给真实的服务地址。

## TODO LIST

* Filter 定义 → 提供了 com.sinri.passover.VertxHttp.AbstractRequestFilter 这个类作为抽象以供继承，可以多个连续进行。
* SDK 化 → 直接使用 com.sinri.passover.VertxHttp.VertxHttpGateway 类即可，Passover可以视为是利用此SDK实现的封装。
* 请求映射，类似于路由 → 提供了 com.sinri.passover.VertxHttp.BasePassoverRouter 这个类进行转发（魔改端口，http为80，https为443）。可以写子类替换。
