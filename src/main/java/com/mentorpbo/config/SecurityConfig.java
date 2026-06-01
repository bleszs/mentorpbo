package com.mentorpbo.config;

import com.mentorpbo.service.CustomOAuth2UserService;
import com.mentorpbo.service.OAuth2SuccessHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @Value("${app.google.oauth2.enabled:false}")
    private boolean googleOAuth2Enabled;

    @Value("${spring.security.oauth2.client.registration.google.client-id:PLACEHOLDER}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:PLACEHOLDER}")
    private String googleClientSecret;

    /**
     * Google Client ID yang valid selalu berakhir dengan .apps.googleusercontent.com
     * dan client secret tidak boleh placeholder.
     */
    private boolean isOAuth2Ready() {
        return googleOAuth2Enabled
            && googleClientId != null
            && googleClientId.endsWith(".apps.googleusercontent.com")
            && googleClientSecret != null
            && !googleClientSecret.startsWith("GANTI")
            && !googleClientSecret.equals("PLACEHOLDER");
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

        if (isOAuth2Ready()) {
            http.oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                .failureUrl("/login?error=oauth")
            );
        } else {
            // Blokir /oauth2/authorization/** agar tidak diteruskan ke Google dengan credentials kosong/salah
            http.addFilterBefore(new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request,
                                                HttpServletResponse response,
                                                FilterChain chain) throws ServletException, IOException {
                    if (request.getRequestURI().startsWith("/oauth2/authorization/")) {
                        response.sendRedirect(request.getContextPath() + "/login?error=oauth-not-configured");
                        return;
                    }
                    chain.doFilter(request, response);
                }
            }, SecurityContextHolderFilter.class);
        }

        return http.build();
    }
}
