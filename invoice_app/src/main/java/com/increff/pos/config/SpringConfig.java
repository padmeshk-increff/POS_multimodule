package com.increff.pos.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
        "com.increff.pos.controller",
        "com.increff.pos.dto",
        "com.increff.pos.flow"
})
@Import({SwaggerConfig.class})
public class SpringConfig {

}
