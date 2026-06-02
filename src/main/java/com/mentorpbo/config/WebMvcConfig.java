package com.mentorpbo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * WebMvcConfig — konfigurasi tambahan Spring MVC.
 *
 * Memetakan URL /uploads/** ke folder uploads/ di filesystem (working directory),
 * sehingga foto profil dan file yang diunggah bisa diakses via browser.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ambil path absolut folder uploads dari working directory
        String uploadPath = Paths.get("uploads/").toAbsolutePath().toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);

        String pesanPath = Paths.get("uploads/pesan/").toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/pesan/**")
                .addResourceLocations(pesanPath);
    }
}
