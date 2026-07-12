package com.oglimmer.wiki.dto;

import com.oglimmer.wiki.entity.Page;
import java.time.Instant;
import java.util.List;

public record PageDto(
        String slug,
        String title,
        String content,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt,
        String updatedBy) {

    public static PageDto from(Page page) {
        return new PageDto(
                page.getSlug(),
                page.getTitle(),
                page.getContent(),
                page.getTags().stream().sorted().toList(),
                page.getCreatedAt(),
                page.getUpdatedAt(),
                page.getUpdatedBy());
    }
}
