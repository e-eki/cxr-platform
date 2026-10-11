package io.github.eeki.cxr.caseservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Базовый класс для всех JPA-сущностей проекта.
 *
 * <p>Содержит только аудиторские поля ({@code createdAt}, {@code updatedAt}),
 * которые заполняются автоматически при сохранении и обновлении сущности.
 * Идентификатор сущности остаётся в конкретной сущности — так каждая из них
 * может иметь свою стратегию генерации и свой тип id.</p>
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    /** Дата и время создания записи. Заполняется один раз при вставке. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Дата и время последнего обновления записи. Обновляется при каждом изменении. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Проставляет {@code createdAt} и {@code updatedAt} перед первой вставкой. */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Обновляет {@code updatedAt} перед каждым изменением сущности. */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}