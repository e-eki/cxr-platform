package io.github.eeki.cxr.caseservice.service;

import io.github.eeki.cxr.caseservice.dto.CaseResponse;
import io.github.eeki.cxr.caseservice.dto.CreateCaseRequest;
import io.github.eeki.cxr.caseservice.dto.ImageUploadedEvent;
import io.github.eeki.cxr.caseservice.model.Case;
import io.github.eeki.cxr.caseservice.model.CaseStatus;
import io.github.eeki.cxr.caseservice.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Сервис для работы с кейсами.
 *
 * <p>Пока умеет создавать кейсы и отдавать их список/детали. На следующих
 * итерациях здесь появятся: загрузка файла в MinIO, публикация события
 * в Kafka и приём результата от ML-сервиса.</p>
 */
@Service
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;
    private final MinioService minioService;
    private final CaseEventProducer eventProducer;

    /**
     * Создаёт новый кейс в статусе {@link CaseStatus#UPLOADED}.
     *
     * @param request метаданные кейса
     * @return созданный кейс
     */
    @Transactional
    public CaseResponse create(CreateCaseRequest request, MultipartFile file) {
        String imageKey = "cases/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        minioService.uploadFile(file, imageKey);

        Case newCase = Case.builder()
                .patientId(request.patientId())
                .imageKey(imageKey)
                .status(CaseStatus.UPLOADED)
                .build();
        Case saved = caseRepository.save(newCase);

        // Публикуем событие после сохранения
        eventProducer.publishImageUploaded(new ImageUploadedEvent(
                saved.getId(),
                saved.getImageKey(),
                saved.getCreatedAt()
        ));

        return toResponse(saved);
    }

    /**
     * Возвращает страницу кейсов, опционально отфильтрованных по статусу.
     *
     * @param status   опциональный фильтр
     * @param pageable параметры пагинации
     */
    @Transactional(readOnly = true)
    public Page<CaseResponse> findAll(CaseStatus status, Pageable pageable) {
        Page<Case> page = (status == null)
                ? caseRepository.findAll(pageable)
                : caseRepository.findByStatus(status, pageable);
        return page.map(this::toResponse);
    }

    /**
     * Возвращает кейс по id.
     *
     * @throws IllegalArgumentException если кейс не найден
     */
    @Transactional(readOnly = true)
    public CaseResponse findById(UUID id) {
        Case found = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + id));
        return toResponse(found);
    }

    /** Маппинг сущности в DTO. */
    private CaseResponse toResponse(Case c) {
        return new CaseResponse(
                c.getId(),
                c.getPatientId(),
                c.getImageKey(),
                c.getStatus(),
                c.getComment(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
