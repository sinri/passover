package io.github.sinri.passover.router;

import io.github.sinri.keel.core.logger.KeelLogger;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public abstract class PassoverFilter {
    private KeelLogger logger;
    private Buffer bodyBuffer;

    public static PassoverFilter factory(String filterName, List<String> params, KeelLogger logger) {
        Class<?>[] classes = new Class[params.size()];
        for (var i = 0; i < params.size(); i++) {
            classes[i] = String.class;
        }

        String class_name = "io.github.sinri.passover.router.filters." + filterName;
        try {
            Object[] objects = params.toArray();
            PassoverFilter passoverFilter = (PassoverFilter) PassoverFilter.class.getClassLoader()
                    .loadClass(class_name)
                    .getConstructor(classes)
                    .newInstance(objects);
            passoverFilter.setLogger(logger);
            return passoverFilter;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 过滤请求；
     * 如果检查符合通过条件，则不respond，返回true；
     * 如果不符合，则直接respond一个终止报文（比如302到登录），并返回false。
     *
     * @param httpServerRequest HttpServerRequest
     * @return passover or not
     */
    abstract public boolean passoverOrNot(HttpServerRequest httpServerRequest);

    public KeelLogger getLogger() {
        return logger;
    }

    public PassoverFilter setLogger(KeelLogger logger) {
        this.logger = logger;
        return this;
    }

    public Buffer getBodyBuffer() {
        return bodyBuffer;
    }

    public PassoverFilter setBodyBuffer(Buffer bodyBuffer) {
        this.bodyBuffer = bodyBuffer;
        return this;
    }
}
