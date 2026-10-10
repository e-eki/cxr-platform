package io.github.eeki.cxr.userservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Запрос на аутентификацию.
 *
 * <p>Содержит логин и пароль в открытом виде. Пароль сверяется
 * с BCrypt-хэшем из БД.</p>
 */
public record LoginRequest(

        /** Логин пользователя. */
        @NotBlank(message = "Username is required")
        String username,

        /** Пароль в открытом виде. */
        @NotBlank(message = "Password is required")
        String password
) {}
