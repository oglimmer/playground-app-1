package com.oglimmer.wiki.service;

import com.oglimmer.wiki.dto.AttachmentDto;
import com.oglimmer.wiki.entity.Page;
import com.oglimmer.wiki.entity.PageAttachment;
import com.oglimmer.wiki.exception.NotFoundException;
import com.oglimmer.wiki.repository.PageAttachmentRepository;
import com.oglimmer.wiki.repository.PageRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PageAttachmentService {

    private final PageAttachmentRepository attachmentRepository;
    private final PageRepository pageRepository;

    @Transactional(readOnly = true)
    public List<AttachmentDto> listAttachments(String slug) {
        Page page = requirePageBySlug(slug);
        return attachmentRepository.findByPageIdOrderByCreatedAtAsc(page.getId()).stream()
                .map(AttachmentDto::from)
                .toList();
    }

    @Transactional
    public AttachmentDto upload(String slug, String filename, String contentType, byte[] data) {
        Page page = requirePageBySlug(slug);
        PageAttachment attachment = new PageAttachment(
                UUID.randomUUID(),
                page,
                filename,
                contentType,
                data.length,
                data,
                Instant.now());
        return AttachmentDto.from(attachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public PageAttachment getAttachment(String slug, UUID attachmentId) {
        Page page = requirePageBySlug(slug);
        return attachmentRepository.findById(attachmentId)
                .filter(a -> a.getPage().getId().equals(page.getId()))
                .orElseThrow(() -> new NotFoundException("Attachment not found: " + attachmentId));
    }

    @Transactional
    public void deleteAttachment(String slug, UUID attachmentId) {
        Page page = requirePageBySlug(slug);
        PageAttachment attachment = attachmentRepository.findById(attachmentId)
                .filter(a -> a.getPage().getId().equals(page.getId()))
                .orElseThrow(() -> new NotFoundException("Attachment not found: " + attachmentId));
        attachmentRepository.delete(attachment);
    }

    private Page requirePageBySlug(String slug) {
        return pageRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Page not found: " + slug));
    }
}
