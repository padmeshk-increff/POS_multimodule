package com.increff.pos.config;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;

public class WebInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[] { SpringConfig.class };
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[] {};
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] { "/" };
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        // Create a multipart config element
        MultipartConfigElement multipartConfigElement = new MultipartConfigElement(null, -1L, -1L, 0);
        registration.setMultipartConfig(multipartConfigElement);
    }
    
    @Override
    protected Filter[] getServletFilters() {
        // This ensures the security filter is registered
        return new Filter[]{
            new DelegatingFilterProxy("springSecurityFilterChain")
        };
    }

}