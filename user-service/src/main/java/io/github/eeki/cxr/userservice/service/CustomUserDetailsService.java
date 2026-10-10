package io.github.eeki.cxr.userservice.service;

import io.github.eeki.cxr.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Загружает пользователя из БД по логину для Spring Security.
 *
 * <p>Вынесен в отдельный бин, чтобы разорвать циклическую зависимость:
 * {@code UserService} зависит от {@code AuthenticationManager}, а тот —
 * от {@link UserDetailsService}. Если бы {@code UserService} сам реализовывал
 * этот интерфейс, Spring не смог бы собрать контекст.</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /** Ищет пользователя по логину; если не найден — бросает {@link UsernameNotFoundException}. */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));
    }
}
