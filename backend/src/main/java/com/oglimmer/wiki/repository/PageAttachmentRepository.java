package com.oglimmer.wiki.repository;

import com.oglimmer.wiki.entity.PageAttachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PageAttachmentRepository extends JpaRepository<PageAttachment, UUID> {
    List<PageAttachment> findByPageIdOrderByCreatedAtAsc(UUID pageId);
}
