package com.ingsis.snippetManager;

import com.ingsis.snippetManager.intermediate.UserAuthorizationService;
import com.ingsis.snippetManager.intermediate.azureStorageConfig.StorageService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import com.ingsis.snippetManager.intermediate.userRoles.Roles;
import com.ingsis.snippetManager.snippet.ValidationResult;
import com.ingsis.snippetManager.snippet.SnippetController;
import com.ingsis.snippetManager.snippet.SnippetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//PASSED ALL THE TEST FOR THE CONTROLLER
@AutoConfigureMockMvc
@WebMvcTest(SnippetController.class)
@ActiveProfiles("test")
class SnippetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SnippetService snippetService;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private UserAuthorizationService userAuthorizationService;

    // -------------------------------------------------------------------
    // US1 - Crear Snippet desde archivo
    // -------------------------------------------------------------------
    @Test
    @WithMockUser
    @DisplayName("US1 - POST /snippets/create/file: creación exitosa")
    void createSnippetFromFile_Success() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "code.js", "text/javascript", "console.log('ok');".getBytes());

        when(userAuthorizationService.validRole(any(), eq(Roles.DEVELOPER))).thenReturn(true); // true → rol válido
        when(storageService.upload(any(), any(), any())).thenReturn("https://fakeurl");
        when(snippetService.createSnippet(any(), anyString())).thenReturn(new ValidationResult(true, "Passed"));

        mockMvc.perform(multipart("/snippets/create/file")
                        .file(file)
                        .param("name", "Snippet 1")
                        .param("description", "desc")
                        .param("language", "JS")
                        .param("version", "1.0")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "user-123"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("US1 - POST /snippets/create/file: parser inválido")
    void createSnippetFromFile_InvalidParser() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "bad.js", "text/javascript", "console.log(".getBytes());

        when(userAuthorizationService.validRole(any(), eq(Roles.DEVELOPER))).thenReturn(true);
        when(storageService.upload(any(), any(), any())).thenReturn("https://fakeurl");
        when(snippetService.createSnippet(any(), anyString()))
                .thenReturn(new ValidationResult(UUID.randomUUID(), false, "Unexpected EOF", 1, 15));

        mockMvc.perform(multipart("/snippets/create/file")
                        .file(file)
                        .param("name", "Invalid snippet")
                        .param("description", "desc")
                        .param("language", "JS")
                        .param("version", "1.0")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "user-123"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("line: 1")));
    }

    // -------------------------------------------------------------------
    // US3 - Crear Snippet desde texto (editor)
    // -------------------------------------------------------------------
    @Test
    @WithMockUser
    @DisplayName("US3 - POST /snippets/create/text: éxito")
    void createSnippetFromText_Success() throws Exception {
        String json = """
            {
              "name": "Snippet text",
              "description": "desc",
              "language": "JS",
              "version": "1.0",
              "content": "console.log('ok');"
            }
            """;

        when(userAuthorizationService.validRole(any(), eq(Roles.DEVELOPER))).thenReturn(true);
        when(storageService.upload(any(), any(), any())).thenReturn("https://fakeurl");
        when(snippetService.createSnippet(any(), anyString())).thenReturn(new ValidationResult(true, "Passed"));

        mockMvc.perform(post("/snippets/create/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "user-123"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("US3 - POST /snippets/create/text: parser inválido")
    void createSnippetFromText_InvalidParser() throws Exception {
        String json = """
            {
              "name": "Snippet text",
              "description": "desc",
              "language": "JS",
              "version": "1.0",
              "content": "console.log("
            }
            """;

        when(userAuthorizationService.validRole(any(), eq(Roles.DEVELOPER))).thenReturn(true);
        when(storageService.upload(any(), any(), any())).thenReturn("https://fakeurl");
        when(snippetService.createSnippet(any(), anyString()))
                .thenReturn(new ValidationResult(UUID.randomUUID(), false, "Missing )", 1, 12));

        mockMvc.perform(post("/snippets/create/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "user-123"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Missing")));
    }

    // -------------------------------------------------------------------
    // US2 - Actualizar desde archivo
    // -------------------------------------------------------------------
    @Test
    @WithMockUser
    @DisplayName("US2 - PUT /snippets/{id}/update/file: éxito")
    void updateSnippetFromFile_Success() throws Exception {

        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "code.js",
                "text/javascript",
                "console.log('ok');".getBytes()
        );


        when(userAuthorizationService.validRole(any(), eq(Roles.DEVELOPER))).thenReturn(true);
        when(storageService.upload(any(), any(), any())).thenReturn("https://fakeurl");
        when(snippetService.updateSnippet(eq(id), any(), anyString()))
                .thenReturn(new ValidationResult(true, "Passed"));


        mockMvc.perform(multipart("/snippets/" + id + "/update/file")
                        .file(file)
                        .param("name", "Update Test")
                        .param("description", "desc")
                        .param("language", "JS")
                        .param("version", "1.0")
                        .with(req -> { req.setMethod("PUT"); return req; })
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "user-123"))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Passed")));
    }

    // -------------------------------------------------------------------
    // US4 - Actualizar desde texto
    // -------------------------------------------------------------------
    @Test
    @WithMockUser
    @DisplayName("US4 - PUT /snippets/{id}/update/text: parser inválido")
    void updateSnippetFromText_InvalidParser() throws Exception {
        UUID id = UUID.randomUUID();
        String json = """
            {
              "name": "Updated",
              "description": "desc",
              "language": "JS",
              "version": "1.0",
              "content": "console.log("
            }
            """;

        when(userAuthorizationService.validRole(any(), eq(Roles.DEVELOPER))).thenReturn(true);
        when(storageService.upload(any(), any(), any())).thenReturn("https://fakeurl");
        when(snippetService.updateSnippet(eq(id), any(), anyString()))
                .thenReturn(new ValidationResult(UUID.randomUUID(), false, "Unexpected EOF", 1, 10));

        mockMvc.perform(put("/snippets/" + id + "/update/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "user-123"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Unexpected EOF")));
    }
}
