package io.github.eeki.cxr.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Маршруты API Gateway.
 *
 * <p>Gateway — единая точка входа для фронтенда. Все запросы идут сюда,
 * а Gateway проксирует их к соответствующим микросервисам. URL сервисов
 * задаются через переменные окружения (в Docker — имена контейнеров,
 * локально — localhost).</p>
 */
@Configuration
public class GatewayRoutesConfig {

    @Value("${app.services.user-service-url}")
    private String userServiceUrl;

    @Value("${app.services.case-service-url}")
    private String caseServiceUrl;

    @Value("${app.services.ml-inference-url}")
    private String mlInferenceUrl;

    /** Определяет все маршруты приложения. */
    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r
                        .path("/api/auth/**")
                        .uri(userServiceUrl))

                .route("case-service", r -> r
                        .path("/api/cases/**")
                        .uri(caseServiceUrl))

                .route("ml-inference", r -> r
                        .path("/api/predict/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(mlInferenceUrl))

                .build();
    }
}
