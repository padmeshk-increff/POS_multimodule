package com.increff.pos.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        DbConfig.class
})
@ComponentScan(basePackages = {
        "com.increff.pos.dao",
})
public class TestDbConfig {

}