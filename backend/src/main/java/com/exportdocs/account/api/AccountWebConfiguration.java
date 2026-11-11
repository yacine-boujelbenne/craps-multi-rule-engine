package com.exportdocs.account.api;

import java.util.UUID;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods=false)
public class AccountWebConfiguration implements WebMvcConfigurer {
    @Override public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class,UUID.class,value->{
            if(!value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
                throw new IllegalArgumentException("Expected a hyphenated UUID.");
            return UUID.fromString(value);
        });
    }
}
