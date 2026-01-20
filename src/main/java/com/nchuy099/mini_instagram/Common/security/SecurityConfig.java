package com.nchuy099.mini_instagram.common.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    private static final String[] SWAGGER_ENDPOINTS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/webjars/**"
    };

    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/sign-up",
            "/auth/login",
            "/auth/refresh-token",
            "/auth/forgot-password",
    };

    private final CustomJwtDecoder customJwtDecoder;

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(
                        SWAGGER_ENDPOINTS)
                .csrf(csrf -> csrf.disable())
                .oauth2ResourceServer(oauth2 -> oauth2.disable()) // DISABLE OAUTH2 RESOURCE SERVER

                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())

        ;

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain publicApiChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(
                        PUBLIC_ENDPOINTS)
                // CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // API → tắt CSRF
                .csrf(csrf -> csrf.disable())

                // Stateless (JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .oauth2ResourceServer(oauth2 -> oauth2.disable()) // DISABLE OAUTH2 RESOURCE SERVER

                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {

        http
                // CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // API → tắt CSRF
                .csrf(csrf -> csrf.disable())

                // Stateless (JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // OAuth2 Resource Server
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(customJwtDecoder)
                        // .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                // Authorization
                .authorizeHttpRequests(auth -> auth
                        // .requestMatchers(PUBLIC_ENDPOINTS).permitAll() // Đã tách ở trên
                        .anyRequest().authenticated());

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // @Bean
    // JwtAuthenticationConverter jwtAuthenticationConverter() {
    // JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new
    // JwtGrantedAuthoritiesConverter();
    // jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");
    // jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("scope");

    // JwtAuthenticationConverter jwtAuthenticationConverter = new
    // JwtAuthenticationConverter();
    // jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

    // return jwtAuthenticationConverter;
    // }

}
