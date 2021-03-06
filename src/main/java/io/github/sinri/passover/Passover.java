package io.github.sinri.passover;

import io.github.sinri.passover.gateway.VertxHttpGateway;
import io.github.sinri.passover.gateway.config.ConfigManager;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.CommandLine;
import io.vertx.core.cli.TypedOption;
import io.vertx.core.logging.LoggerFactory;

import java.util.Arrays;

public class Passover {
    /**
     * 作为可运行JAR包在命令行启动，运行如
     * java -jar passover.jar -c /path/to/config-dir
     * 目前只需要一个参数，也就是配置目录
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        try {
            CLI cli = CLI.create("Passover")
                    .setSummary("神秘的HTTP网关")
                    .addOption(
                            new TypedOption<String>()
                                    .setLongName("config-dir")
                                    .setShortName("c")
                                    .setDescription("给定配置文件夹，否则使用默认配置。")
                                    .setRequired(false)
                                    .setType(String.class)
                    );
            CommandLine commandLine = cli.parse(Arrays.asList(args));
            if (!commandLine.isValid() && commandLine.isAskingForHelp()) {
                StringBuilder builder = new StringBuilder();
                cli.usage(builder);
                System.err.println(builder.toString());
                System.exit(1);
            }

            String configDir = commandLine.getOptionValue("config-dir");
            VertxHttpGateway.initializeVertx(new ConfigManager(configDir));
        } catch (Exception e) {
            LoggerFactory.getLogger(Passover.class).error("Passover 初始化失败", e);
            System.exit(2);
        }

        new VertxHttpGateway().run();
    }
}
