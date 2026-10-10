package io.github.eeki.cxr.userservice.dto;

import io.github.eeki.cxr.userservice.model.Role;

import java.util.UUID;

/**
 * Публичное представление пользователя.
 *
 * <p>Содержит только те поля, которые безопасно возвращать клиенту.
 * Пароль и служебные поля аудита сюда не попадают.</p>
 */
public record UserResponse(

        /** Уникальный идентификатор. */
        UUID id,

        /** Логин. */
        String username,

        /** Email. */
        String email,

        /** Роль пользователя. */
        Role role
) {}
