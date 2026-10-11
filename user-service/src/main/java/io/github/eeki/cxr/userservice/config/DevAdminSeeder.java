package io.github.eeki.cxr.userservice.config;

import io.github.eeki.cxr.userservice.model.Role;
import io.github.eeki.cxr.userservice.model.User;
import io.github.eeki.cxr.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Создаёт дефолтного администратора при первом запуске в dev-профиле.
 *
 * <p>Пароль хэшируется BCrypt «на лету», а не хранится в SQL как готовая
 * строка. Если пользователь с таким логином уже есть — ничего не делает.</p>
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevAdminSeeder implements CommandLineRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername(ADMIN_USERNAME)) {
            return;
        }
        User admin = User.builder()
                .username(ADMIN_USERNAME)
                .email("admin@cxr.local")
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);
    }
}