# Passover

A simple web gateway based on Vert.X 3.

Let Vert.x be the HTTP server, when requests come, filter them and send to configured target server, and pass the responses to clients.

## Basic Usage

### Run as daemon

You need

1. the packaged jar file;
2. a directory containing config files
    1. passover.yml : the general config;
    2. router.yml : define your routes, if you declare to use ConfigDriveRouter in passover.yml

and run as the following.

```bash
java -jar passover.jar -c /path/to/config-dir
```

Sample of passover.yml

```yaml
# Passover
# How many worker threads you need for passover?
workerPoolSize: 40
# Which port you want to listen on local environment.
localListenPort: 8000
# Define your router, which should be an instance of BasePassoverRouter or its extension.
routerClass: io.github.sinri.passover.gateway.ConfigDriveRouter
# If you want use CAS to check incoming requests, you have to register the service name.
casServiceName: oms-xxl-passover
```

Sample of router.yml

```yaml
# ConfigDriveRouter
routerName: DemoRouter
# Rules are checked when a request comes by order
rules:
  -
    conditions:
      host: www.sample.com
      path: .*
    route:
      # domain is optional
      # serviceHostForProxy and servicePortForProxy should be fit for passover machine to connect target, 
      # and if useHttpsForProxy then servicePortForProxy might ought to be 443
      serviceHostForProxy: 127.0.0.1
      servicePortForProxy: 8080
      useHttpsForProxy: false
      # Use this parameter to decide if the requests match this conditions should be abandoned
      shouldBeAbandoned: false
      # Use this parameter to inform the framework that if there were any filters need parse body, such as those contain token inside body
      shouldFilterWithBody: false
      # The list of filter classes, use full class path with namespace
      filterClasses:
        # A sample is Leqee CAS
        -
          class: io.github.sinri.passover.sample.Plugin.LeqeeCASFilter
          config:
            aa_tp_code: oms-xxl-passover
```

---

基本的实现

* Filter 定义 → 提供了 com.sinri.passover.VertxHttp.AbstractRequestFilter 这个类作为抽象以供继承，可以多个连续进行。
* SDK 化 → 直接使用 com.sinri.passover.VertxHttp.VertxHttpGateway 类即可，Passover可以视为是利用此SDK实现的封装。
* 请求映射，类似于路由 → 提供了 com.sinri.passover.VertxHttp.BasePassoverRouter 这个类进行转发（魔改端口，http为80，https为443）。可以写子类替换。

初步的应用

* Service端无身份机制，利用Passover限制非AA登录的场景（未登录需要进行登陆页转发） → FirstRouter + LeqeeCommonAuthFilter
* Service端有身份验证机制，利用Passover预先校验，消灭恶意请求（未登录的拒否）→ TODO 暂时没有实验场景
* 监控访问频率 → 提供了一个AbstractRequestStatFilter类作为标准Filter实现，可以参考sample
