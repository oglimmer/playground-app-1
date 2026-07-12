package com.oglimmer.wiki.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SavePageRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull @Size(max = 1_000_000) String content,
        List<@Size(max = 64) String> tags) {}
