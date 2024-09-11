package com.example.order_service.config;

import com.example.order_service.jwt.JWTAccessDeniedHandler;
import com.example.order_service.jwt.JwtAuthenticationEntryPoint;
import com.example.order_service.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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
                .authorizeHttpRequests(req->req
                .requestMatchers(HttpMethod.POST, "/order/**").hasRole("USER")
                .requestMatchers(HttpMethod.GET, "/order/**").hasRole("USER")

               .anyRequest().authenticated()
                )
                .exceptionHandling(
                        e->e.accessDeniedHandler(accessDeniedHandler)
                                .authenticationEntryPoint(authenticationEntryPoint)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web)
                -> web.ignoring().requestMatchers("/authenticate/signup", "/authenticate/login", "/authenticate/refreshtoken");
    }

}
