package com.mentorpbo.config;

import com.mentorpbo.service.CustomOAuth2UserService;
import com.mentorpbo.service.OAuth2SuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig - Mengkonfigurasi Spring Security untuk:
 * 1. Mengizinkan semua request (autentikasi manual via HttpSession)
 * 2. Mengaktifkan Google OAuth2 login
 * 3. Menonaktifkan CSRF (tidak diperlukan karena form Thymeleaf + manual auth)
 * 4. Mengizinkan H2 console (frame-options)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Matikan CSRF — kita gunakan autentikasi manual via session
            .csrf(csrf -> csrf.disable())

            // Izinkan iframe untuk H2 console
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )

            // Izinkan semua request — logika auth ditangani controller
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )

            // Matikan form login bawaan Spring Security
            .formLogin(form -> form.disable())

            // Matikan HTTP Basic
            .httpBasic(basic -> basic.disable())

            // Matikan logout Spring Security (kita punya /logout sendiri)
            .logout(logout -> logout.disable())

            // Konfigurasi OAuth2 Login (Google)
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2SuccessHandler)
                .failureUrl("/login?error=oauth")
            );

        return http.build();
    }
}
