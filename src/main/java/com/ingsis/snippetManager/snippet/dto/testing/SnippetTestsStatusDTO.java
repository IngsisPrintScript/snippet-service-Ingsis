package com.ingsis.snippetManager.snippet.dto.testing;

import com.ingsis.snippetManager.redis.testing.dto.TestReturnDTO;

import java.util.List;
import java.util.UUID;

public record SnippetTestsStatusDTO(UUID snippetId,
                                    String snippetName,
                                    List<TestValidateDTO> tests) {
}
