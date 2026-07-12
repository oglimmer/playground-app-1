package com.oglimmer.wiki.dto;

import com.oglimmer.wiki.entity.Page;
import java.time.Instant;
import java.util.List;

public record PageSummaryDto(String slug, String title, List<String> tags, Instant updatedAt, String updatedBy) {

    public static PageSummaryDto from(Page page) {
        return new PageSummaryDto(
                page.getSlug(),
                page.getTitle(),
                page.getTags().stream().sorted().toList(),
                page.getUpdatedAt(),
                page.getUpdatedBy());
    }
}
