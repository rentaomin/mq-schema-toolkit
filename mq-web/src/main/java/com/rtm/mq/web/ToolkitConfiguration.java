package com.rtm.mq.web;

import com.rtm.mq.runtime.ConversionOptions;
import com.rtm.mq.runtime.MessageCodec;
import com.rtm.mq.runtime.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for toolkit beans.
 */
@Configuration
@EnableConfigurationProperties(ToolkitProperties.class)
public class ToolkitConfiguration {
    @Bean
    public MessageCodec messageCodec(ToolkitProperties properties) {
        ConversionOptions options = new ConversionOptions();
        options.setProtocolConfig(properties.getProtocol());
        try {
            options.setGroupIdMode(Enum.valueOf(com.rtm.mq.runtime.GroupIdMode.class, properties.getGroupIdMode()));
        } catch (IllegalArgumentException ignored) {
        }
        return new MessageConverter(options);
    }
}
