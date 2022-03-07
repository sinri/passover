package io.github.sinri.passover.router;

import io.github.sinri.keel.core.logger.KeelLogger;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public abstract class PassoverFilter {
    private KeelLogger logger;
    private Buffer bodyBuffer;

    /**
     * @param filterName 可以是io.github.sinri.passover.router.filters包下的类名或相对路径，也可以是包外的绝对路径
     * @param params     参数列表，每个元素都是String
     * @param logger     日志记录器
     * @return 如果能找到filter就返回相应的实例。找不到或者出错就是null。
     */
    public static PassoverFilter factory(String filterName, List<String> params, KeelLogger logger) {
        Class<?>[] classes = new Class[params.size()];
        for (var i = 0; i < params.size(); i++) {
            classes[i] = String.class;
        }

        Class<?> clx;
        try {
            String class_name = "io.github.sinri.passover.router.filters." + filterName;
            clx = PassoverFilter.class.getClassLoader().loadClass(class_name);
        } catch (ClassNotFoundException e) {
            try {
                clx = PassoverFilter.class.getClassLoader().loadClass(filterName);
            } catch (ClassNotFoundException ex) {
                return null;
            }
        }

        try {
            Object[] objects = params.toArray();
            PassoverFilter passoverFilter = (PassoverFilter) clx.getConstructor(classes).newInstance(objects);
            passoverFilter.setLogger(logger);
            return passoverFilter;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
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
