package com.oglimmer.wiki.dto;

import com.oglimmer.wiki.entity.Page;
import java.time.Instant;

public record PageDto(
        String slug, String title, String content, Instant createdAt, Instant updatedAt, String updatedBy) {

    public static PageDto from(Page page) {
        return new PageDto(
                page.getSlug(),
                page.getTitle(),
                page.getContent(),
                page.getCreatedAt(),
                page.getUpdatedAt(),
                page.getUpdatedBy());
    }
}
