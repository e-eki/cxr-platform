package io.github.eeki.cxr.caseservice.repository;

import io.github.eeki.cxr.caseservice.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для работы с ревью кейсов.
 */
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /** Возвращает все ревью для кейса, отсортированные по дате (новые сверху). */
    List<Review> findByCaseRefIdOrderByCreatedAtDesc(UUID caseId);
}