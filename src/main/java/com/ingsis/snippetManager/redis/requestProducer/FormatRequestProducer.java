package com.ingsis.snippetManager.redis.requestProducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingsis.snippetManager.redis.dto.request.FormatRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class FormatRequestProducer {

    private static final Logger logger = LoggerFactory.getLogger(FormatRequestProducer.class);

    private final String streamName;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public FormatRequestProducer(@Value("${redis.streams.formatRequest}") String streamName,
            RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.streamName = streamName;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(FormatRequestEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.opsForStream().add(StreamRecords.newRecord().ofObject(json).withStreamKey(streamName));
            logger.info("Published FormatRequestEvent for Snippet({})", event.snippetId());
        } catch (Exception ex) {
            logger.error("Error publishing FormatRequestEvent", ex);
        }
    }
}
