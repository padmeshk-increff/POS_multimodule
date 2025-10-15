package com.increff.pos.config;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;

public class WebInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    //TODO: read about all the functions
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[] {};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[] { SpringConfig.class };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] { "/" };
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        // Create a multipart config element
        // The parameters are:
        // 1. location: A temporary directory to store files. null means use the server's default.
        // 2. maxFileSize: The maximum size for a single file. -1L means no limit.
        // 3. maxRequestSize: The maximum size for the entire multipart request. -1L means no limit.
        // 4. fileSizeThreshold: The size threshold after which files will be written to disk.
        MultipartConfigElement multipartConfigElement = new MultipartConfigElement(null, -1L, -1L, 0);

        // Set the multipart config on the servlet registration
        registration.setMultipartConfig(multipartConfigElement);
    }
}