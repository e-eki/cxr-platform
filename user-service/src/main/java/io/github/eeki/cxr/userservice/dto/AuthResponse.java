package io.github.eeki.cxr.userservice.dto;

/**
 * Ответ на успешную аутентификацию или регистрацию.
 *
 * <p>Содержит JWT-токен, который клиент должен передавать
 * в заголовке {@code Authorization: Bearer &lt;token&gt;}.</p>
 */
public record AuthResponse(

        /** JWT-токен доступа. */
        String token
) {}
