package io.github.eeki.cxr.caseservice.service;

import io.github.eeki.cxr.caseservice.dto.InferenceCompletedEvent;
import io.github.eeki.cxr.caseservice.dto.InferenceFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka-консьюмер, слушает результаты ML-инференса и обновляет кейсы.
 *
 * <p>Каждое событие обрабатывается в своём listener'е с типизированным
 * payload. Обработка идемпотентна: повторная доставка того же события
 * не создаёт дублей — predictions кейса сначала удаляются, потом
 * создаются заново.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InferenceResultConsumer {

    private final CaseService caseService;

    /** Обрабатывает успешный инференс. */
    @KafkaListener(
            topics = "${app.kafka.topics.inference-completed}",
            containerFactory = "inferenceCompletedListenerFactory"
    )
    public void onInferenceCompleted(InferenceCompletedEvent event) {
        log.info("Received InferenceCompletedEvent for case {} ({} predictions, {} ms)",
                event.caseId(), event.predictions().size(), event.durationMs());
        try {
            caseService.applyInferenceResult(event);
        } catch (Exception e) {
            log.error("Failed to apply inference result for case {}", event.caseId(), e);
            // Пробрасываем, чтобы Spring Kafka не подтверждал offset
            // и попробовал ещё раз согласно политике error handler
            throw e;
        }
    }

    /** Обрабатывает ошибку инференса. */
    @KafkaListener(
            topics = "${app.kafka.topics.inference-failed}",
            containerFactory = "inferenceFailedListenerFactory"
    )
    public void onInferenceFailed(InferenceFailedEvent event) {
        log.warn("Received InferenceFailedEvent for case {}: {}",
                event.caseId(), event.error());
        try {
            caseService.markAsFailed(event);
        } catch (Exception e) {
            log.error("Failed to mark case {} as FAILED", event.caseId(), e);
            throw e;
        }
    }
}