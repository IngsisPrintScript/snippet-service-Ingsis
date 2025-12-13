package com.ingsis.snippetManager.redis.requestProducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingsis.snippetManager.redis.dto.request.TestRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class TestRequestProducer {

    private static final Logger logger = LoggerFactory.getLogger(TestRequestProducer.class);

    private final String streamName;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public TestRequestProducer(@Value("${redis.streams.testRequest}") String streamName, StringRedisTemplate redis,
            ObjectMapper objectMapper) {
        this.streamName = streamName;
        this.redisTemplate = redis;
        this.objectMapper = objectMapper;
    }

    public void publish(TestRequestEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            ObjectRecord<String, String> record = StreamRecords.newRecord().ofObject(json).withStreamKey(streamName);

            redisTemplate.opsForStream().add(record);
            logger.info("Published TestRequestEvent for Snippet({})", event.snippetId());
        } catch (Exception ex) {
            logger.error("Error publishing TestRequestEvent", ex);
        }
    }
}
