package io.github.eeki.cxr.caseservice.service;

import io.github.eeki.cxr.caseservice.dto.CaseResponse;
import io.github.eeki.cxr.caseservice.dto.CaseReviewedEvent;
import io.github.eeki.cxr.caseservice.dto.CreateCaseRequest;
import io.github.eeki.cxr.caseservice.dto.ImageUploadedEvent;
import io.github.eeki.cxr.caseservice.dto.InferenceCompletedEvent;
import io.github.eeki.cxr.caseservice.dto.InferenceFailedEvent;
import io.github.eeki.cxr.caseservice.dto.ReviewRequest;
import io.github.eeki.cxr.caseservice.dto.ReviewResponse;
import io.github.eeki.cxr.caseservice.model.Case;
import io.github.eeki.cxr.caseservice.model.CaseStatus;
import io.github.eeki.cxr.caseservice.model.Prediction;
import io.github.eeki.cxr.caseservice.model.Review;
import io.github.eeki.cxr.caseservice.repository.CaseRepository;
import io.github.eeki.cxr.caseservice.repository.PredictionRepository;
import io.github.eeki.cxr.caseservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Сервис для работы с кейсами.
 *
 * <p>Отвечает за создание кейсов, загрузку снимков в объектное хранилище,
 * публикацию событий в Kafka и обработку результатов ML-инференса.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaseService {

    private final CaseRepository caseRepository;
    private final PredictionRepository predictionRepository;
    private final MinioService minioService;
    private final CaseEventProducer eventProducer;
    private final ReviewRepository reviewRepository;

    /**
     * Создаёт новый кейс в статусе {@link CaseStatus#UPLOADED},
     * загружает снимок в хранилище и публикует событие для ML-сервиса.
     *
     * @param request метаданные кейса
     * @param file    загруженный снимок
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

        eventProducer.publishImageUploaded(new ImageUploadedEvent(
                saved.getId(),
                saved.getImageKey(),
                saved.getCreatedAt()
        ));

        log.info("Created case {} for patient {}", saved.getId(), saved.getPatientId());
        return toResponse(saved);
    }

    /**
     * Возвращает страницу кейсов, опционально отфильтрованных по статусу.
     */
    @Transactional(readOnly = true)
    public Page<CaseResponse> findAll(CaseStatus status, Pageable pageable) {
        Page<Case> page = (status == null)
                ? caseRepository.findAll(pageable)
                : caseRepository.findByStatus(status, pageable);
        return page.map(this::toResponse);
    }

    /**
     * Возвращает кейс по id вместе с предсказаниями ML-модели.
     */
    @Transactional(readOnly = true)
    public CaseResponse findById(UUID id) {
        Case found = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + id));
        List<Prediction> predictions = predictionRepository
                .findByCaseRefIdOrderByConfidenceDesc(id);
        return toResponse(found, predictions);
    }

    /** Возвращает содержимое снимка из хранилища. */
    @Transactional(readOnly = true)
    public byte[] getImageBytes(UUID id) {
        Case caseEntity = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + id));
        return minioService.downloadFile(caseEntity.getImageKey());
    }

    /**
     * Применяет результат успешного ML-инференса: сохраняет bbox-ы
     * и переводит кейс в статус READY_FOR_REVIEW.
     *
     * <p>Метод идемпотентен: повторный вызов с теми же данными не создаёт
     * дублей — старые predictions удаляются перед вставкой новых.</p>
     */
    @Transactional
    public void applyInferenceResult(InferenceCompletedEvent event) {
        Case caseEntity = caseRepository.findById(event.caseId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Case not found: " + event.caseId()));

        predictionRepository.deleteByCaseRefId(event.caseId());

        List<Prediction> predictions = event.predictions().stream()
                .map(p -> Prediction.builder()
                        .caseRef(caseEntity)
                        .className(p.className())
                        .confidence(p.confidence())
                        .xMin(p.xMin())
                        .yMin(p.yMin())
                        .xMax(p.xMax())
                        .yMax(p.yMax())
                        .build())
                .toList();
        predictionRepository.saveAll(predictions);

        caseEntity.setStatus(CaseStatus.READY_FOR_REVIEW);
        caseEntity.setComment(null);
        caseRepository.save(caseEntity);

        log.info("Applied {} predictions to case {}, status -> READY_FOR_REVIEW",
                predictions.size(), event.caseId());
    }

    /**
     * Помечает кейс как FAILED при ошибке инференса.
     */
    @Transactional
    public void markAsFailed(InferenceFailedEvent event) {
        Case caseEntity = caseRepository.findById(event.caseId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Case not found: " + event.caseId()));

        caseEntity.setStatus(CaseStatus.FAILED);
        caseEntity.setComment("Inference failed: " + event.error());
        caseRepository.save(caseEntity);

        log.warn("Case {} marked as FAILED: {}", event.caseId(), event.error());
    }

    /**
     * Обрабатывает ревью кейса врачом.
     *
     * <p>Сохраняет решение, переводит кейс в статус REVIEWED
     * и публикует событие в Kafka для аудита.</p>
     */
    @Transactional
    public ReviewResponse review(UUID caseId, ReviewRequest request, String reviewerId) {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + caseId));

        if (caseEntity.getStatus() != CaseStatus.READY_FOR_REVIEW) {
            throw new IllegalStateException(
                    "Case " + caseId + " is not ready for review (status: "
                            + caseEntity.getStatus() + ")");
        }

        Review review = Review.builder()
                .caseRef(caseEntity)
                .reviewerId(reviewerId)
                .decision(request.decision())
                .comment(request.comment())
                .build();
        Review saved = reviewRepository.save(review);

        // Обновляем статус кейса
        CaseStatus newStatus = (request.decision() == Review.Decision.CONFIRMED)
                ? CaseStatus.CONFIRMED
                : CaseStatus.REJECTED;
        caseEntity.setStatus(newStatus);
        caseEntity.setComment(request.comment());
        caseRepository.save(caseEntity);

        // Публикуем событие
        eventProducer.publishCaseReviewed(new CaseReviewedEvent(
                caseEntity.getId(),
                reviewerId,
                request.decision(),
                saved.getCreatedAt()
        ));

        log.info("Case {} reviewed by {}: {}",
                caseId, reviewerId, request.decision());

        return new ReviewResponse(
                saved.getId(),
                saved.getReviewerId(),
                saved.getDecision(),
                saved.getComment(),
                saved.getCreatedAt()
        );
    }

    /** Маппинг сущности в DTO без предсказаний (для списка). */
    private CaseResponse toResponse(Case c) {
        long count = predictionRepository.countByCaseRefId(c.getId());
        return new CaseResponse(
                c.getId(),
                c.getPatientId(),
                c.getImageKey(),
                c.getStatus(),
                c.getComment(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                (int) count,
                List.of()
        );
    }

    /** Маппинг сущности в DTO с предсказаниями (для деталей). */
    private CaseResponse toResponse(Case c, List<Prediction> predictions) {
        List<CaseResponse.PredictionResponse> predictionDtos = predictions.stream()
                .map(p -> new CaseResponse.PredictionResponse(
                        p.getClassName(),
                        p.getConfidence(),
                        p.getXMin(),
                        p.getYMin(),
                        p.getXMax(),
                        p.getYMax()))
                .toList();

        return new CaseResponse(
                c.getId(),
                c.getPatientId(),
                c.getImageKey(),
                c.getStatus(),
                c.getComment(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                predictionDtos.size(),
                predictionDtos
        );
    }
}