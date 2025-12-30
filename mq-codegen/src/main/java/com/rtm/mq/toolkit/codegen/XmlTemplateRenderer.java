package com.rtm.mq.toolkit.codegen;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;

/**
 * Renders XML using a FreeMarker template.
 */
public class XmlTemplateRenderer {
    public String render(XmlMessageModel model, XmlTemplateConfig config) throws IOException {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
        configuration.setDefaultEncoding("UTF-8");
        if (config.getTemplatePath() != null && !config.getTemplatePath().isBlank()) {
            Path path = Path.of(config.getTemplatePath());
            configuration.setTemplateLoader(new FileTemplateLoader(path.getParent().toFile()));
            return renderTemplate(configuration, path.getFileName().toString(), model);
        }
        configuration.setTemplateLoader(new ClassTemplateLoader(getClass().getClassLoader(), "/templates"));
        return renderTemplate(configuration, "converter-xml.ftl", model);
    }

    private String renderTemplate(Configuration configuration, String name, XmlMessageModel model) throws IOException {
        try {
            Template template = configuration.getTemplate(name);
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (TemplateException ex) {
            throw new IOException("Failed to render XML template: " + ex.getMessage(), ex);
        }
    }
}
