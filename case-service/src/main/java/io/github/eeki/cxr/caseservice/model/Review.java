package io.github.eeki.cxr.caseservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Ревью кейса врачом — финальное решение по результату ML-анализа.
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review extends BaseEntity {

    /** Уникальный идентификатор ревью. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Кейс, который ревьюится. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseRef;

    /** Идентификатор врача (из user-service). */
    @Column(name = "reviewer_id", nullable = false, length = 64)
    private String reviewerId;

    /** Решение: подтверждено или отклонено. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Decision decision;

    /** Комментарий врача (опционально). */
    @Column(columnDefinition = "TEXT")
    private String comment;

    /** Возможные решения по результату ML-анализа. */
    public enum Decision {
        CONFIRMED,
        REJECTED
    }
}
