package io.github.eeki.cxr.caseservice.repository;

import io.github.eeki.cxr.caseservice.model.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для работы с предсказаниями ML-модели.
 */
public interface PredictionRepository extends JpaRepository<Prediction, UUID> {

    /** Возвращает все предсказания для кейса, отсортированные по уверенности (убывание). */
    List<Prediction> findByCaseRefIdOrderByConfidenceDesc(UUID caseId);

    /** Удаляет все предсказания кейса (перед повторным инференсом). */
    void deleteByCaseRefId(UUID caseId);

    /** Возвращает количество предсказаний для кейса. */
    long countByCaseRefId(UUID caseId);
}
