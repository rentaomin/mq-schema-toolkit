package com.rtm.mq.cli;

import picocli.CommandLine;

import java.nio.file.Path;

/**
 * Shared CLI options.
 */
public abstract class BaseCommand implements Runnable {
    @CommandLine.Option(names = "--baseDir", description = "Base directory for schema repo.",
            defaultValue = "schema-repo-example")
    protected Path baseDir;

    @CommandLine.Option(names = "--schemaDir", description = "Schema directory override.")
    protected Path schemaDir;

    protected Path resolveSchemaDir() {
        if (schemaDir != null) {
            return schemaDir;
        }
        return baseDir.resolve("schemas");
    }

    protected Path resolveConfigDir() {
        return baseDir.resolve("config");
    }
}
