package io.github.sinri.passover.gateway.config;

import io.vertx.core.logging.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

public class ConfigManager {

    private File configDir;

    public ConfigManager(String configDirPath) throws IOException {
        configDir = new File(configDirPath);
        if (!configDir.exists() || !configDir.isDirectory() || !configDir.canRead()) {
            throw new IOException("给定的配置目录不可用");
        }
    }

    public Map<String, Object> fetchYaml(String fileKey) throws FileNotFoundException {
        InputStream input = new FileInputStream(new File(configDir.getPath() + "/" + fileKey + ".yml"));
        Yaml yaml = new Yaml();
        return yaml.load(input);
    }

    public PassoverConfig getPassoverConfig() {
        try {
            Map<String, Object> passover = fetchYaml("passover");
            return new PassoverConfig(passover);
        } catch (FileNotFoundException e) {
            LoggerFactory.getLogger(this.getClass()).error("Cannot get config file, use default", e);
            return new PassoverConfig();
        }
    }
}
