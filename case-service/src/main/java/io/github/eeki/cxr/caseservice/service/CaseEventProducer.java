package io.github.eeki.cxr.caseservice.service;

import io.github.eeki.cxr.caseservice.dto.CaseReviewedEvent;
import io.github.eeki.cxr.caseservice.dto.ImageUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Публикует события о жизненном цикле кейса в Kafka.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaseEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.image-uploaded}")
    private String imageUploadedTopic;

    @Value("${app.kafka.topics.case-reviewed}")
    private String caseReviewedTopic;

    /**
     * Публикует событие «снимок загружен».
     *
     * <p>Key = caseId, чтобы все события одного кейса попадали в одну
     * партицию и обрабатывались строго по порядку.</p>
     */
    public void publishImageUploaded(ImageUploadedEvent event) {
        kafkaTemplate.send(imageUploadedTopic, event.caseId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published ImageUploadedEvent for case {} to {}",
                                event.caseId(), imageUploadedTopic);
                    } else {
                        log.error("Failed to publish ImageUploadedEvent for case {}",
                                event.caseId(), ex);
                    }
                });
    }

    /** Публикует событие о завершении ревью. */
    public void publishCaseReviewed(CaseReviewedEvent event) {
        kafkaTemplate.send(caseReviewedTopic, event.caseId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published CaseReviewedEvent for case {}", event.caseId());
                    } else {
                        log.error("Failed to publish CaseReviewedEvent for case {}",
                                event.caseId(), ex);
                    }
                });
    }
}
