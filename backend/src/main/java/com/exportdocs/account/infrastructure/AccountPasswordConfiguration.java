package com.exportdocs.account.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;

@Configuration(proxyBeanMethods=false)
public class AccountPasswordConfiguration {
    @Bean PasswordEncoder accountPasswordEncoder() { return PasswordEncoderFactories.createDelegatingPasswordEncoder(); }
    @Bean Jackson2ObjectMapperBuilderCustomizer strictAccountJson() {
        return builder->builder.featuresToEnable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION,DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT,MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .postConfigurer(mapper->{
                    var textual=mapper.coercionConfigFor(LogicalType.Textual);
                    for(var shape:new CoercionInputShape[]{CoercionInputShape.Integer,CoercionInputShape.Float,CoercionInputShape.Boolean})
                        textual.setCoercion(shape,CoercionAction.Fail);
                });
    }
}
