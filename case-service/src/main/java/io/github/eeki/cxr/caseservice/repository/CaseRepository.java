package io.github.eeki.cxr.caseservice.repository;

import io.github.eeki.cxr.caseservice.model.Case;
import io.github.eeki.cxr.caseservice.model.CaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Репозиторий для работы с кейсами.
 *
 * <p>Помимо стандартных CRUD-операций умеет искать кейсы по статусу
 * с пагинацией — это нужно для экрана «Список кейсов на ревью».</p>
 */
public interface CaseRepository extends JpaRepository<Case, UUID> {

    /** Возвращает страницу кейсов с заданным статусом. */
    Page<Case> findByStatus(CaseStatus status, Pageable pageable);
}
