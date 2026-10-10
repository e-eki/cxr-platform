package io.github.eeki.cxr.userservice.model;

/**
 * Роли пользователей в системе.
 *
 * <p>Используется в Spring Security для проверки прав доступа:
 * роль превращается в authority с префиксом {@code ROLE_} (например, {@code ROLE_DOCTOR}).</p>
 */
public enum Role {

    /** Врач: может загружать снимки и проводить ревью. */
    DOCTOR,

    /** Администратор: управляет пользователями и настройками системы. */
    ADMIN
}