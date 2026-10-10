package io.github.eeki.cxr.userservice.repository;

import io.github.eeki.cxr.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с пользователями.
 *
 * <p>Наследует стандартные CRUD-операции от {@link JpaRepository}
 * и добавляет методы поиска по уникальным полям.</p>
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Находит пользователя по логину. */
    Optional<User> findByUsername(String username);

    /** Проверяет, занят ли логин. */
    boolean existsByUsername(String username);

    /** Проверяет, зарегистрирован ли email. */
    boolean existsByEmail(String email);
}
