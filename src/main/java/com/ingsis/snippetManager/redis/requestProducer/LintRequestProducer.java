package com.ingsis.snippetManager.redis.requestProducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingsis.snippetManager.redis.dto.request.LintRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class LintRequestProducer {

    private static final Logger logger = LoggerFactory.getLogger(LintRequestProducer.class);

    private final String streamName;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public LintRequestProducer(@Value("${redis.streams.lintRequest}") String streamName,
            RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.streamName = streamName;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(LintRequestEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            ObjectRecord<String, String> record = StreamRecords.newRecord().ofObject(json).withStreamKey(streamName);
            redisTemplate.opsForStream().add(record);
            logger.info("Published LintRequestEvent for Snippet({})", event.snippetId());
        } catch (Exception ex) {
            logger.error("Error publishing LintRequestEvent", ex);
        }
    }
}
