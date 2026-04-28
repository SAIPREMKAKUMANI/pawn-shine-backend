package com.project.pawn.customeronboarding.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Expose the /projects/pawn-images directory to be accessible via /api/images/**
        registry.addResourceHandler("/api/images/**")
                .addResourceLocations("file:/projects/pawn-images/");
    }
}
