package io.github.eeki.cxr.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Конфигурация цепочки фильтров Spring Security.
 *
 * <p>Приложение stateless: сессии не создаются, вся аутентификация идёт
 * через JWT-токен. Эндпоинты {@code /api/auth/**} открыты, остальные
 * требуют аутентификации.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Настраивает правила доступа и отключает сессии. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }
}