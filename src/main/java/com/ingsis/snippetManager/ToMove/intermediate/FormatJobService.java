package com.ingsis.snippetManager.ToMove.intermediate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class FormatJobService {
  private final RedisTemplate<String, Object> redisTemplate;

  public FormatJobService(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public String createJob(List<UUID> snippetIds) {
    String jobId = UUID.randomUUID().toString();

    redisTemplate.opsForHash().put("formatJob:" + jobId, "status", Status.PENDING);

    snippetIds.forEach(
        id ->
            redisTemplate
                .opsForHash()
                .put("formatJob:" + jobId + ":snippets", id.toString(), Status.PENDING));

    return jobId;
  }

  public void updateSnippetStatus(String jobId, UUID snippetId, Status status) {
    redisTemplate
        .opsForHash()
        .put("formatJob:" + jobId + ":snippets", snippetId.toString(), status);
  }

  public Map<Object, Object> getJobStatus(String jobId) {
    return redisTemplate.opsForHash().entries("formatJob:" + jobId + ":snippets");
  }

  public String getJobGlobalStatus(String jobId) {
    return (String) redisTemplate.opsForHash().get("formatJob:" + jobId, "status");
  }
}
