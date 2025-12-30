package com.rtm.mq.toolkit.cli;

import picocli.CommandLine;

/**
 * CLI entry point for mq-schema-toolkit.
 */
@CommandLine.Command(
        name = "mqtool",
        mixinStandardHelpOptions = true,
        version = "mq-schema-toolkit 0.0.1",
        description = "Schema-driven MQ fixed-length toolkit.",
        subcommands = {
                ImportExcelCommand.class,
                ValidateCommand.class,
                GenJavaCommand.class,
                GenXmlCommand.class,
                GenOpenApiCommand.class,
                DiffCommand.class
        }
)
public class MqTool implements Runnable {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new MqTool()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
