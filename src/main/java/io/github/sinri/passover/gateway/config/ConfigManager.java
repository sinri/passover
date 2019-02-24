package io.github.sinri.passover.gateway.config;

import io.vertx.core.logging.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

public class ConfigManager {

    private File configDir;

    public ConfigManager(String configDirPath) {
        try {
            configDir = null;
            if (configDirPath == null) throw new Exception("未给定配置目录，将使用默认的配置");
            configDir = new File(configDirPath);
            if (!configDir.exists() || !configDir.isDirectory() || !configDir.canRead()) {
                throw new IOException("给定的配置目录不可用");
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass()).warn("初始化加载配置文件目录出现问题", e);
        }
    }

    public Map<String, Object> fetchYaml(String fileKey) throws FileNotFoundException {
        InputStream input = new FileInputStream(new File(configDir.getPath() + "/" + fileKey + ".yml"));
        Yaml yaml = new Yaml();
        return yaml.load(input);
    }

    public PassoverConfig getPassoverConfig() {
        try {
            if (configDir == null) throw new Exception("未给定配置目录");
            Map<String, Object> passover = fetchYaml("passover");
            return new PassoverConfig(passover);
        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass()).warn("因故将使用默认的Passover配置", e);
            return new PassoverConfig();
        }
    }

    public RouterConfig getRouterConfig() {
        try {
            if (configDir == null) throw new Exception("未给定配置目录");
            Map<String, Object> map = fetchYaml("router");
            return new RouterConfig(map);
        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass()).warn("因故将使用默认的Router配置", e);
            return null;
        }
    }
}
