package io.github.eeki.cxr.caseservice.controller;

import io.github.eeki.cxr.caseservice.dto.CaseResponse;
import io.github.eeki.cxr.caseservice.dto.CreateCaseRequest;
import io.github.eeki.cxr.caseservice.dto.ReviewRequest;
import io.github.eeki.cxr.caseservice.dto.ReviewResponse;
import io.github.eeki.cxr.caseservice.model.CaseStatus;
import io.github.eeki.cxr.caseservice.service.CaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST-контроллер для работы с кейсами.
 *
 * <p>Пока без авторизации — она появится после интеграции с API Gateway.
 * В продакшене эти эндпоинты будут доступны только аутентифицированным
 * пользователям с ролью DOCTOR или ADMIN.</p>
 */
@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    /** Создаёт новый кейс. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CaseResponse> create(
            @RequestPart("patientId") String patientId,
            @RequestPart("file") MultipartFile file) {

        CreateCaseRequest request = new CreateCaseRequest(patientId, null);
        return ResponseEntity.ok(caseService.create(request, file));
    }

    /** Возвращает страницу кейсов с опциональным фильтром по статусу. */
    @GetMapping
    public ResponseEntity<Page<CaseResponse>> list(
            @RequestParam(required = false) CaseStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                caseService.findAll(status, PageRequest.of(page, size)));
    }

    /** Возвращает кейс по id. */
    @GetMapping("/{id}")
    public ResponseEntity<CaseResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(caseService.findById(id));
    }

    /**
     * Стримит снимок кейса из объектного хранилища.
     */
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable UUID id) {
        byte[] image = caseService.getImageBytes(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    /**
     * Проводит ревью кейса. Reviewer определяется по заголовку X-User-Id,
     * который добавляет API Gateway после валидации JWT.
     */
    @PostMapping("/{id}/review")
    public ResponseEntity<ReviewResponse> review(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String reviewerId) {
        return ResponseEntity.ok(caseService.review(id, request, reviewerId));
    }
}
