package com.oglimmer.wiki.dto;

import com.oglimmer.wiki.entity.Page;
import java.time.Instant;
import java.util.List;

public record PageDto(
        String slug,
        String title,
        String content,
        List<String> tags,
        List<AttachmentDto> attachments,
        Instant createdAt,
        Instant updatedAt,
        String updatedBy) {

    public static PageDto from(Page page) {
        return new PageDto(
                page.getSlug(),
                page.getTitle(),
                page.getContent(),
                page.getTags().stream().sorted().toList(),
                List.of(), // populated by PageService after fetching attachments
                page.getCreatedAt(),
                page.getUpdatedAt(),
                page.getUpdatedBy());
    }
}
