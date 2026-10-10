package io.github.eeki.cxr.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Конфигурация общих бинов аутентификации.
 *
 * <p>Провайдер {@code DaoAuthenticationProvider} создаётся автоматически
 * Spring Security: он находит бин {@link org.springframework.security.core.userdetails.UserDetailsService}
 * ({@code CustomUserDetailsService}) и {@link PasswordEncoder}.</p>
 */
@Configuration
public class ApplicationConfig {

    /** BCrypt-энкодер для хэширования паролей. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Менеджер аутентификации, используемый в сервисах. */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}