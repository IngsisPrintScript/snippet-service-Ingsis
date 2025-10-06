package com.ingsis.snippetManager.ToMove.snippet.dto;

import com.ingsis.snippetManager.ToMove.intermediate.Status;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FormatJobStatus {

  private String jobId;
  private Status status;
  private Map<UUID, String> snippets = new HashMap<>();

  public FormatJobStatus(String jobId) {
    this.jobId = jobId;
    this.status = Status.PENDING;
  }

  public String getJobId() {
    return jobId;
  }

  public Status getStatus() {
    return status;
  }

  public Map<UUID, String> getSnippets() {
    return snippets;
  }
}
