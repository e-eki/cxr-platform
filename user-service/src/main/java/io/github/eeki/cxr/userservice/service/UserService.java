package io.github.eeki.cxr.userservice.service;

import io.github.eeki.cxr.userservice.dto.AuthResponse;
import io.github.eeki.cxr.userservice.dto.LoginRequest;
import io.github.eeki.cxr.userservice.dto.RegisterRequest;
import io.github.eeki.cxr.userservice.dto.UserResponse;
import io.github.eeki.cxr.userservice.model.Role;
import io.github.eeki.cxr.userservice.model.User;
import io.github.eeki.cxr.userservice.repository.UserRepository;
import io.github.eeki.cxr.userservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для работы с пользователями.
 *
 * <p>Отвечает за регистрацию новых пользователей и проверку учётных данных
 * при логине. Возвращает JWT-токены, которые клиент использует для
 * последующих запросов.</p>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Регистрирует нового пользователя.
     *
     * @param request данные для регистрации
     * @return JWT-токен для немедленного использования
     * @throws IllegalArgumentException если логин или email уже заняты
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.DOCTOR)
                .build();

        userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(user));
    }

    /**
     * Проверяет логин и пароль, возвращает JWT-токен.
     *
     * @param request учётные данные
     * @return JWT-токен
     * @throws org.springframework.security.core.AuthenticationException если логин/пароль неверны
     */
    @Transactional(readOnly = true)
    public AuthResponse authenticate(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new AuthResponse(jwtService.generateToken(user));
    }

    /**
     * Возвращает публичные данные пользователя по логину.
     *
     * @param username логин
     * @return публичное представление пользователя
     */
    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new UserResponse(user.getId(), user.getUsername(),
                user.getEmail(), user.getRole());
    }
}