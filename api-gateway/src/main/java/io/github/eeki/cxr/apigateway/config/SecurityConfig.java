package io.github.eeki.cxr.apigateway.config;

import io.github.eeki.cxr.apigateway.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Конфигурация безопасности Gateway.
 *
 * <p>Вся проверка JWT выполняется в {@link JwtAuthenticationFilter}, который
 * явно встроен в цепочку Spring Security через {@code addFilterBefore}.
 * Сам SecurityFilterChain пропускает все запросы ({@code permitAll}) —
 * если токен невалиден, наш фильтр вернёт 401 раньше, чем запрос дойдёт
 * до проксирования.</p>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** Внедрение зависимости через конструктор (без Lombok). */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /** Настраивает цепочку с JWT-фильтром на входе. */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .addFilterBefore(jwtAuthenticationFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}