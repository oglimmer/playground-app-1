package com.oglimmer.wiki.repository;

import com.oglimmer.wiki.dto.AttachmentDto;
import com.oglimmer.wiki.entity.PageAttachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PageAttachmentRepository extends JpaRepository<PageAttachment, UUID> {

    @Query("SELECT new com.oglimmer.wiki.dto.AttachmentDto(a.id, a.filename, a.contentType, a.size)"
            + " FROM PageAttachment a WHERE a.page.id = :pageId ORDER BY a.createdAt ASC")
    List<AttachmentDto> findMetadataByPageIdOrderByCreatedAtAsc(@Param("pageId") UUID pageId);
}
