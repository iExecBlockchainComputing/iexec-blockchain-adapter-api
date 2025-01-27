package com.iexec.blockchain.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfigurationAdapter {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                /*
                    By default Spring Security protects any incoming POST
                    (or PUT/DELETE/PATCH) request with a valid CSRF token
                    Will eventually activate it later.
                */
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.and())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).anonymous() // Anonymous swagger access
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/prometheus"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic();
        return http.build();
    }

}
