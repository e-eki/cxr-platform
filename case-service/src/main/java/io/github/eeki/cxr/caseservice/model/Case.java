package io.github.eeki.cxr.caseservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Кейс — рентгеновский снимок, загруженный врачом для анализа.
 *
 * <p>Хранит метаданные снимка и текущий статус обработки. Сам файл
 * изображения лежит в объектном хранилище (MinIO), здесь хранится
 * только ключ объекта.</p>
 */
@Entity
@Table(name = "cases")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Case extends BaseEntity {

    /** Уникальный идентификатор кейса. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Анонимизированный идентификатор пациента (например, внешний ID из HIS). */
    @Column(name = "patient_id", nullable = false, length = 64)
    private String patientId;

    /** Ключ объекта в MinIO/S3, по которому можно достать снимок. */
    @Column(name = "image_key", nullable = false, length = 255)
    private String imageKey;

    /** Текущий статус обработки кейса. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CaseStatus status;

    /** Дополнительный комментарий врача или сообщение об ошибке. */
    @Column(columnDefinition = "TEXT")
    private String comment;
}
