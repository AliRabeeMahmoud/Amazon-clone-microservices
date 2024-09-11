package com.example.product_service.config;


import com.example.product_service.jwt.JWTAccessDeniedHandler;
import com.example.product_service.jwt.JwtAuthenticationEntryPoint;
import com.example.product_service.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity(debug = true)
@RequiredArgsConstructor
public class SecurityConfig {
    //
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JWTAccessDeniedHandler accessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeRequests(req->req
                        .requestMatchers(HttpMethod.POST, "/product/**").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/product/**").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/product/**").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/product/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(
                        e->e.accessDeniedHandler(accessDeniedHandler)
                                .authenticationEntryPoint(authenticationEntryPoint)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web)
                -> web.ignoring().requestMatchers("/authenticate/signup", "/authenticate/login", "/authenticate/refreshtoken");
    }

}
