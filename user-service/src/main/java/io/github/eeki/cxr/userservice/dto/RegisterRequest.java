package io.github.eeki.cxr.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Запрос на регистрацию нового пользователя.
 *
 * <p>Валидируется Jakarta Bean Validation: логин 3–64 символа,
 * корректный email, пароль не короче 6 символов.</p>
 */
public record RegisterRequest(

        /** Логин пользователя. */
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 64)
        String username,

        /** Email пользователя. */
        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        String email,

        /** Пароль в открытом виде. Хэшируется перед сохранением. */
        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password
) {}
