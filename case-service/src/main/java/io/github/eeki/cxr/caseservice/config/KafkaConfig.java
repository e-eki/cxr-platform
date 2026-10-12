package io.github.eeki.cxr.caseservice.config;

import io.github.eeki.cxr.caseservice.dto.InferenceCompletedEvent;
import io.github.eeki.cxr.caseservice.dto.InferenceFailedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурация Kafka-консьюмеров для обработки результатов ML-инференса.
 *
 * <p>Определяет две фабрики listener-контейнеров — по одной на каждый
 * тип события. Это позволяет строго типизировать payload и не смешивать
 * completed/failed сообщения в одном listener'е.</p>
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /** Фабрика listener'ов для событий inference.completed. */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InferenceCompletedEvent>
    inferenceCompletedListenerFactory() {
        return createFactory(InferenceCompletedEvent.class, "inference-completed");
    }

    /** Фабрика listener'ов для событий inference.failed. */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InferenceFailedEvent>
    inferenceFailedListenerFactory() {
        return createFactory(InferenceFailedEvent.class, "inference-failed");
    }

    /**
     * Создаёт фабрику listener'ов с указанным типом payload.
     */
    private <T> ConcurrentKafkaListenerContainerFactory<String, T> createFactory(
            Class<T> targetType, String clientIdSuffix) {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "case-service-" + clientIdSuffix);

        JsonDeserializer<T> deserializer = new JsonDeserializer<>(targetType, false);
        deserializer.addTrustedPackages("io.github.eeki.cxr.caseservice.dto");

        DefaultKafkaConsumerFactory<String, T> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);

        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}