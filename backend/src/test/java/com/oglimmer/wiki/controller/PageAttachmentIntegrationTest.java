package com.oglimmer.wiki.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oglimmer.wiki.entity.AppUser;
import com.oglimmer.wiki.entity.Role;
import com.oglimmer.wiki.entity.UserStatus;
import com.oglimmer.wiki.repository.AppUserRepository;
import com.oglimmer.wiki.service.CurrentUserService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class PageAttachmentIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    private AppUser approvedUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        approvedUser = new AppUser(
                UUID.randomUUID(),
                "test-subject",
                "test@example.com",
                "Test User",
                Role.USER,
                UserStatus.APPROVED,
                Instant.now());
        org.mockito.Mockito.when(currentUserService.requireApproved()).thenReturn(approvedUser);
    }

    @Test
    @WithMockUser
    void uploadListDownloadAndDeleteAttachment() throws Exception {
        // Create a page first
        Map<String, Object> pageRequest = Map.of(
                "title", "Attachment Test Page",
                "content", "Some content",
                "tags", List.of());
        String pageJson = objectMapper.writeValueAsString(pageRequest);

        var createResult = mockMvc.perform(post("/pages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pageJson))
                .andExpect(status().isCreated())
                .andReturn();
        String slug = objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("slug")
                .asText();

        // Upload a text file attachment
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello, World!".getBytes());
        mockMvc.perform(multipart("/pages/{slug}/attachments", slug).file(file).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("test.txt"))
                .andExpect(jsonPath("$.contentType").value("text/plain"))
                .andExpect(jsonPath("$.size").value(13));

        // List attachments
        var listResult = mockMvc.perform(get("/pages/{slug}/attachments", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].filename").value("test.txt"))
                .andReturn();
        String attachmentId = objectMapper
                .readTree(listResult.getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asText();

        // Download the attachment data
        mockMvc.perform(get("/pages/{slug}/attachments/{id}/data", slug, attachmentId))
                .andExpect(status().isOk())
                .andExpect(content().bytes("Hello, World!".getBytes()));

        // Delete the attachment
        mockMvc.perform(delete("/pages/{slug}/attachments/{id}", slug, attachmentId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        // Verify it's gone
        mockMvc.perform(get("/pages/{slug}/attachments", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser
    void uploadImageAttachment() throws Exception {
        // Create a page
        Map<String, Object> pageRequest = Map.of(
                "title", "Image Test Page",
                "content", "Some content",
                "tags", List.of());
        String pageJson = objectMapper.writeValueAsString(pageRequest);

        var createResult = mockMvc.perform(post("/pages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pageJson))
                .andExpect(status().isCreated())
                .andReturn();
        String slug = objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("slug")
                .asText();

        // Upload a fake PNG image
        byte[] fakePng = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", fakePng);
        mockMvc.perform(multipart("/pages/{slug}/attachments", slug).file(file).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("image.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.size").value(8));
    }

    @Test
    @WithMockUser
    void attachmentNotFoundReturns404() throws Exception {
        // Create a page first
        Map<String, Object> pageRequest = Map.of(
                "title", "404 Test Page",
                "content", "Some content",
                "tags", List.of());
        String pageJson = objectMapper.writeValueAsString(pageRequest);

        var createResult = mockMvc.perform(post("/pages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pageJson))
                .andExpect(status().isCreated())
                .andReturn();
        String slug = objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("slug")
                .asText();

        UUID fakeId = UUID.randomUUID();
        mockMvc.perform(get("/pages/{slug}/attachments/{id}/data", slug, fakeId))
                .andExpect(status().isNotFound());
    }
}
