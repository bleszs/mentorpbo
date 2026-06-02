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
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    // Dibaca dari JVM system property -D (diset oleh Dockerfile ENTRYPOINT dari shell env var)
    @Value("${spring.security.oauth2.client.registration.google.client-id:PLACEHOLDER}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:PLACEHOLDER}")
    private String clientSecret;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        String id  = clientId  != null ? clientId.trim()  : "PLACEHOLDER";
        String sec = clientSecret != null ? clientSecret.trim() : "PLACEHOLDER";

        log.info("[OAuth2] client-id  : {}",
                 id.endsWith(".apps.googleusercontent.com")
                     ? id.substring(0, 20) + "...(valid)" : id);
        log.info("[OAuth2] client-sec : {}",
                 (!sec.equals("PLACEHOLDER") && !sec.isBlank()) ? "set" : "PLACEHOLDER/empty");

        if (!id.endsWith(".apps.googleusercontent.com") || sec.equals("PLACEHOLDER") || sec.isBlank()) {
            log.warn("[OAuth2] Credentials not available — OAuth2 disabled");
            return registrationId -> null;
        }

        log.info("[OAuth2] Google OAuth2 configured OK");
        return new InMemoryClientRegistrationRepository(
            ClientRegistration.withRegistrationId("google")
                .clientId(id)
                .clientSecret(sec)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("email", "profile")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .clientName("Google")
                .build()
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                .failureUrl("/login?error=oauth")
            );

        return http.build();
    }
}
