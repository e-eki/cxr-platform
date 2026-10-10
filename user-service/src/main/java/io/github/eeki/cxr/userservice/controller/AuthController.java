package io.github.eeki.cxr.userservice.controller;

import io.github.eeki.cxr.userservice.dto.AuthResponse;
import io.github.eeki.cxr.userservice.dto.LoginRequest;
import io.github.eeki.cxr.userservice.dto.RegisterRequest;
import io.github.eeki.cxr.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер для аутентификации и регистрации.
 *
 * <p>Эндпоинты этого контроллера открыты для всех — они не требуют JWT.
 * Это настроено в {@link io.github.eeki.cxr.user.config.SecurityConfig}.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * Регистрирует нового пользователя и возвращает JWT-токен.
     *
     * @param request данные регистрации
     * @return токен доступа
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    /**
     * Аутентифицирует пользователя и возвращает JWT-токен.
     *
     * @param request логин и пароль
     * @return токен доступа
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.authenticate(request));
    }
}
