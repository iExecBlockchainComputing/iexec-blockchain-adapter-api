package com.iexec.blockchain.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

    protected void configure(HttpSecurity http) throws Exception {
        http
                /*
                    By default Spring Security protects any incoming POST
                    (or PUT/DELETE/PATCH) request with a valid CSRF token
                    Will eventually activate it later.
                 */
                .csrf().disable()
                .cors().and()
                .authorizeRequests()
                .mvcMatchers("/v2/api-docs", // <--- START Anonymous swagger access
                        "/configuration/ui",
                        "/swagger-resources/**",
                        "/configuration/security",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/webjars/**").anonymous() // <--- END Anonymous swagger access
                .anyRequest().authenticated()
                .and()
                .httpBasic();
    }

}
