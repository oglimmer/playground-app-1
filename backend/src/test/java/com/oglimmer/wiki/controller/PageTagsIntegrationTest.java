package com.oglimmer.wiki.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class PageTagsIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void createPageAndFilterByTag() throws Exception {
        Map<String, Object> request = Map.of(
                "title", "My Tagged Page",
                "content", "Some content",
                "tags", List.of("Alpha", "alpha", "beta"));

        mockMvc.perform(post("/pages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tags", containsInAnyOrder("alpha", "beta")));

        mockMvc.perform(get("/pages").param("tag", "alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tags", containsInAnyOrder("alpha", "beta")));

        mockMvc.perform(get("/pages/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", containsInAnyOrder("alpha", "beta")));
    }

    @Test
    @WithMockUser
    void createPageWithInvalidTagIsRejected() throws Exception {
        Map<String, Object> request = Map.of(
                "title", "Bad Tag Page",
                "content", "Some content",
                "tags", List.of("bad tag!"));

        mockMvc.perform(post("/pages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
