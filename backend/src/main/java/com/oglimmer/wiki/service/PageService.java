package com.oglimmer.wiki.service;

import com.oglimmer.wiki.dto.AttachmentDto;
import com.oglimmer.wiki.dto.PageDto;
import com.oglimmer.wiki.dto.PageSummaryDto;
import com.oglimmer.wiki.dto.SavePageRequest;
import com.oglimmer.wiki.entity.Page;
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
public class PageService {

    private final PageRepository pageRepository;
    private final PageAttachmentRepository attachmentRepository;

    @Transactional(readOnly = true)
    public List<PageSummaryDto> list() {
        return pageRepository.findAllByOrderByTitleAsc().stream()
                .map(PageSummaryDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PageSummaryDto> listByTag(String tag) {
        return pageRepository.findAllByTagsContainingOrderByTitleAsc(tag).stream()
                .map(PageSummaryDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listTags() {
        return pageRepository.findDistinctTags();
    }

    @Transactional(readOnly = true)
    public PageDto get(String slug) {
        Page page = requireBySlug(slug);
        List<AttachmentDto> attachments = attachmentRepository.findMetadataByPageIdOrderByCreatedAtAsc(page.getId());
        return new PageDto(
                page.getSlug(),
                page.getTitle(),
                page.getContent(),
                page.getTags().stream().sorted().toList(),
                attachments,
                page.getCreatedAt(),
                page.getUpdatedAt(),
                page.getUpdatedBy());
    }

    @Transactional
    public PageDto create(SavePageRequest request, String editorName) {
        Instant now = Instant.now();
        Page page = new Page(
                UUID.randomUUID(),
                uniqueSlug(Slugs.slugify(request.title())),
                request.title().trim(),
                request.content(),
                now,
                now,
                editorName,
                Tags.normalize(request.tags() == null ? List.of() : request.tags()));
        return PageDto.from(pageRepository.save(page));
    }

    @Transactional
    public PageDto update(String slug, SavePageRequest request, String editorName) {
        Page page = requireBySlug(slug);
        page.setTitle(request.title().trim());
        page.setContent(request.content());
        page.setTags(Tags.normalize(request.tags() == null ? List.of() : request.tags()));
        page.setUpdatedAt(Instant.now());
        page.setUpdatedBy(editorName);
        return PageDto.from(pageRepository.save(page));
    }

    @Transactional
    public void delete(String slug) {
        Page page = requireBySlug(slug);
        pageRepository.delete(page);
    }

    private Page requireBySlug(String slug) {
        return pageRepository.findBySlug(slug).orElseThrow(() -> new NotFoundException("Page not found: " + slug));
    }

    private String uniqueSlug(String base) {
        if (!pageRepository.existsBySlug(base)) {
            return base;
        }
        int suffix = 2;
        while (pageRepository.existsBySlug(base + "-" + suffix)) {
            suffix++;
        }
        return base + "-" + suffix;
    }
}
