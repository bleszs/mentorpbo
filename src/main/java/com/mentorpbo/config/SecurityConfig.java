package com.mentorpbo.config;

import com.mentorpbo.service.CustomOAuth2UserService;
import com.mentorpbo.service.OAuth2SuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired private CustomOAuth2UserService customOAuth2UserService;
    @Autowired private OAuth2SuccessHandler oAuth2SuccessHandler;

    @Value("${spring.security.oauth2.client.registration.google.client-id:PLACEHOLDER}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:PLACEHOLDER}")
    private String clientSecret;

    private boolean isOAuthValid() {
        String id  = clientId  != null ? clientId.trim()  : "";
        String sec = clientSecret != null ? clientSecret.trim() : "";
        boolean ok = id.endsWith(".apps.googleusercontent.com")
                  && !sec.equals("PLACEHOLDER") && !sec.isBlank();
        log.info("[OAuth2] id-valid={} sec-valid={} -> oauth={}",
                 id.endsWith(".apps.googleusercontent.com"),
                 !sec.equals("PLACEHOLDER") && !sec.isBlank(), ok);
        return ok;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable());

        if (isOAuthValid()) {
            log.info("[OAuth2] Enabling Google OAuth2 login");
            http.oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(ui -> ui.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                .failureUrl("/login?error=oauth")
            );
        } else {
            log.warn("[OAuth2] Credentials not valid — OAuth2 login disabled");
        }

        return http.build();
    }
}
