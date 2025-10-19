package com.ingsis.snippetManager.redis.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingsis.snippetManager.redis.format.dto.FormatResultEvent;
import com.ingsis.snippetManager.snippet.SnippetRepo;
import jakarta.annotation.PreDestroy;
import org.austral.ingsis.redis.RedisStreamConsumer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Profile("!test")
public class FormatResultConsumer extends RedisStreamConsumer<String> {

    private static final Logger logger = LoggerFactory.getLogger(FormatResultConsumer.class);
    private final ObjectMapper objectMapper;
    private final SnippetRepo snippetRepository;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public FormatResultConsumer(
            @Value("${redis.streams.formatResult}") String streamName,
            @Value("${redis.groups.format}") String groupName,
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            SnippetRepo snippetRepository
    ) {
        super(streamName, groupName, redisTemplate);
        this.objectMapper = objectMapper;
        this.snippetRepository = snippetRepository;
    }

    @Override
    public void onMessage(@NotNull ObjectRecord<String, String> record) {
        try {
            FormatResultEvent event = objectMapper.readValue(record.getValue(), FormatResultEvent.class);
            logger.info("Received lint result for Snippet({}) - Status: {}", event.snippetId(), event.status());

            snippetRepository.findById(event.snippetId()).ifPresent(snippet -> {
                snippet.setLintStatus(event.status());
                snippetRepository.save(snippet);
            });

        } catch (Exception e) {
            logger.error("Error processing lint result: {}", e.getMessage());
        }
    }

    @Override
    public @NotNull StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, String>> options() {
        return StreamReceiver.StreamReceiverOptions.builder()
                .pollTimeout(java.time.Duration.ofSeconds(10))
                .targetType(String.class)
                .build();
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}