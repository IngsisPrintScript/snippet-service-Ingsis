package com.ingsis.snippetManager.redis.config;

import com.ingsis.snippetManager.redis.dto.result.TestResultEvent;
import com.ingsis.snippetManager.redis.dto.status.SnippetStatus;
import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.SnippetRepo;
import com.ingsis.snippetManager.snippet.TestStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TestResultHandlerService {

    private final SnippetRepo snippetRepository;

    public TestResultHandlerService(SnippetRepo snippetRepository) {
        this.snippetRepository = snippetRepository;
    }

    @Transactional
    public void handle(TestResultEvent event) {

        Snippet snippet = snippetRepository
                .findById(event.snippetId())
                .orElseThrow();

        snippet.addOrUpdateTestStatus(
                event.testId(),
                event.status()
        );
        snippetRepository.save(snippet);
    }
}