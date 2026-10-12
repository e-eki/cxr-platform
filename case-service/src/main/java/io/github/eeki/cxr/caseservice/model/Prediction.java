package io.github.eeki.cxr.caseservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Предсказание ML-модели — координаты одного очага на снимке.
 *
 * <p>Координаты хранятся в нормализованном виде [0, 1], чтобы не зависеть
 * от исходного разрешения снимка. При отображении умножаются на реальные
 * размеры картинки в пикселях.</p>
 */
@Entity
@Table(name = "predictions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prediction extends BaseEntity {

    /** Уникальный идентификатор предсказания. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Кейс, к которому относится предсказание. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseRef;

    /** Название класса (например, "pneumonia"). */
    @Column(name = "class_name", nullable = false, length = 64)
    private String className;

    /** Уверенность модели [0, 1]. */
    @Column(nullable = false)
    private Double confidence;

    /** Нормализованные координаты bbox [0, 1]. */
    @Column(name = "x_min", nullable = false)
    private Double xMin;

    @Column(name = "y_min", nullable = false)
    private Double yMin;

    @Column(name = "x_max", nullable = false)
    private Double xMax;

    @Column(name = "y_max", nullable = false)
    private Double yMax;
}