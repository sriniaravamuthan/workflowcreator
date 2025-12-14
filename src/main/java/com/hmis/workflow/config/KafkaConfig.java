package com.hmis.workflow.config;

import com.hmis.workflow.domain.event.OrderEvent;
import com.hmis.workflow.domain.event.TaskEvent;
import com.hmis.workflow.domain.event.WorkflowEvent;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for workflow engine
 * Configures topics, producers, and consumers for event streaming
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    // ==================== Topics ====================

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic taskEventTopic() {
        return new NewTopic("workflow-task-events", 3, (short) 1);
    }

    @Bean
    public NewTopic orderEventTopic() {
        return new NewTopic("workflow-order-events", 3, (short) 1);
    }

    @Bean
    public NewTopic workflowEventTopic() {
        return new NewTopic("workflow-state-events", 3, (short) 1);
    }

    @Bean
    public NewTopic systemEventTopic() {
        return new NewTopic("system-events", 3, (short) 1);
    }

    // ==================== Producer Factories ====================

    @Bean
    public ProducerFactory<String, TaskEvent> taskEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(taskEventProducerConfigs());
    }

    @Bean
    public ProducerFactory<String, OrderEvent> orderEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(orderEventProducerConfigs());
    }

    @Bean
    public ProducerFactory<String, WorkflowEvent> workflowEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(workflowEventProducerConfigs());
    }

    private Map<String, Object> taskEventProducerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return props;
    }

    private Map<String, Object> orderEventProducerConfigs() {
        return taskEventProducerConfigs();
    }

    private Map<String, Object> workflowEventProducerConfigs() {
        return taskEventProducerConfigs();
    }

    // ==================== Consumer Configs ====================

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, TaskEvent>> taskEventListenerFactory() {
        return createListenerFactory(TaskEvent.class);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, OrderEvent>> orderEventListenerFactory() {
        return createListenerFactory(OrderEvent.class);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, WorkflowEvent>> workflowEventListenerFactory() {
        return createListenerFactory(WorkflowEvent.class);
    }

    private <T> KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, T>> createListenerFactory(Class<T> eventClass) {
        return new org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<String, T>() {
            {
                setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler());
            }
        };
    }

    // Consumer properties helper
    public static Map<String, Object> consumerConfigs(String bootstrapServers, String groupId, Class<?> eventClass) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, eventClass.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return props;
    }
}
