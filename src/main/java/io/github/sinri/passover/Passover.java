package io.github.sinri.passover;

import io.github.sinri.passover.gateway.VertxHttpGateway;
import io.github.sinri.passover.gateway.config.ConfigManager;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.CommandLine;
import io.vertx.core.cli.TypedOption;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

public class Passover {
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
        } catch (IOException e) {
            LoggerFactory.getLogger(Passover.class).error("Passover 初始化失败", e);
            System.exit(2);
        }

        new VertxHttpGateway().run();
    }
}
