package com.mentorpbo.config;

import com.mentorpbo.service.CustomOAuth2UserService;
import com.mentorpbo.service.OAuth2SuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * Baca credentials LANGSUNG dari System.getenv() — melewati Spring property
     * resolution yang sebelumnya mengembalikan string kosong di Railway.
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        String clientId     = System.getenv("GOOGLE_CLIENT_ID");
        String clientSecret = System.getenv("GOOGLE_CLIENT_SECRET");

        log.info("[OAuth2] GOOGLE_CLIENT_ID  = {}",
                 clientId != null ? clientId.substring(0, Math.min(20, clientId.length())) + "..." : "NULL");
        log.info("[OAuth2] GOOGLE_CLIENT_SECRET = {}",
                 clientSecret != null ? "set (" + clientSecret.length() + " chars)" : "NULL");

        if (clientId == null || clientId.isBlank()
                || !clientId.trim().endsWith(".apps.googleusercontent.com")
                || clientSecret == null || clientSecret.isBlank()) {
            log.warn("[OAuth2] Credentials missing/invalid — OAuth2 disabled");
            return registrationId -> null;
        }

        ClientRegistration google = ClientRegistration
                .withRegistrationId("google")
                .clientId(clientId.trim())
                .clientSecret(clientSecret.trim())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("email", "profile")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .clientName("Google")
                .build();

        log.info("[OAuth2] Google OAuth2 configured successfully");
        return new InMemoryClientRegistrationRepository(google);
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
