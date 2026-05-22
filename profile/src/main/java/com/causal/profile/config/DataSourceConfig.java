package com.causal.profile.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry;

import javax.sql.DataSource;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DataSourceConfig {

    @Bean
    BeanPostProcessor dataSourceOtelWrapping(OpenTelemetry openTelemetry) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource ds) {
                    return JdbcTelemetry.create(openTelemetry).wrap(ds);
                }
                return bean;
            }
        };
    }
}
