# Passover

为鲵搞的简单的网关系统。

这个版本基于Vertx。

原理是用Vert.x启动HttpServer，收到HTTP(S)请求时（处理一波之后）重新包装成一个请求发送给真实的服务地址。

## REQUIREMENTS

基本的实现

* Filter 定义 → 提供了 com.sinri.passover.VertxHttp.AbstractRequestFilter 这个类作为抽象以供继承，可以多个连续进行。
* SDK 化 → 直接使用 com.sinri.passover.VertxHttp.VertxHttpGateway 类即可，Passover可以视为是利用此SDK实现的封装。
* 请求映射，类似于路由 → 提供了 com.sinri.passover.VertxHttp.BasePassoverRouter 这个类进行转发（魔改端口，http为80，https为443）。可以写子类替换。

初步的应用

* Service端无身份机制，利用Passover限制非AA登录的场景（未登录需要进行登陆页转发） → FirstRouter + LeqeeCommonAuthFilter
* Service端有身份验证机制，利用Passover预先校验，消灭恶意请求（未登录的拒否）→ TODO 暂时没有实验场景
* 监控访问频率 → TODO
