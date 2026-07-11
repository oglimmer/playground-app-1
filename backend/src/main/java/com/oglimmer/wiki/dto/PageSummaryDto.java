package com.oglimmer.wiki.dto;

import com.oglimmer.wiki.entity.Page;
import java.time.Instant;

public record PageSummaryDto(String slug, String title, Instant updatedAt, String updatedBy) {

    public static PageSummaryDto from(Page page) {
        return new PageSummaryDto(page.getSlug(), page.getTitle(), page.getUpdatedAt(), page.getUpdatedBy());
    }
}
