package com.ingsis.snippetManager.redis.resultConsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingsis.snippetManager.redis.dto.result.TestResultEvent;
import com.ingsis.snippetManager.snippet.SnippetRepo;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.ingsis.snippetManager.redis.config.TestResultHandlerService;
import org.austral.ingsis.redis.RedisStreamConsumer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class TestResultConsumer extends RedisStreamConsumer<String> {

    private static final Logger logger = LoggerFactory.getLogger(TestResultConsumer.class);
    private final ObjectMapper objectMapper;
    private final TestResultHandlerService handler;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public TestResultConsumer(@Value("${redis.streams.testResult}") String streamName,
            @Value("${redis.groups.test}") String groupName,
                              StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper,
                              TestResultHandlerService handler) {
        super(streamName, groupName, redisTemplate);
        this.objectMapper = objectMapper;
        this.handler = handler;
    }

    @Override
    public void onMessage(@NotNull ObjectRecord<String, String> record) {
        executor.submit(() -> {
            try {
                TestResultEvent event =
                        objectMapper.readValue(record.getValue(), TestResultEvent.class);

                handler.handle(event);

            } catch (Exception e) {
                logger.error("Error processing TestResultEvent", e);
            }
        });
    }

    @Override
    public @NotNull StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, String>> options() {
        return StreamReceiver.StreamReceiverOptions.builder().pollTimeout(java.time.Duration.ofSeconds(10))
                .targetType(String.class).build();
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
