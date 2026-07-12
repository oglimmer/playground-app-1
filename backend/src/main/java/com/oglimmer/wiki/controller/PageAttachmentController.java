package com.oglimmer.wiki.controller;

import com.oglimmer.wiki.dto.AttachmentDto;
import com.oglimmer.wiki.entity.PageAttachment;
import com.oglimmer.wiki.service.CurrentUserService;
import com.oglimmer.wiki.service.PageAttachmentService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/pages/{slug}/attachments")
@RequiredArgsConstructor
public class PageAttachmentController {

    private final PageAttachmentService attachmentService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<AttachmentDto> list(@PathVariable String slug) {
        currentUserService.requireApproved();
        return attachmentService.listAttachments(slug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SneakyThrows
    public AttachmentDto upload(@PathVariable String slug, @RequestParam("file") MultipartFile file) {
        currentUserService.requireApproved();
        return attachmentService.upload(
                slug,
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed",
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                file.getBytes());
    }

    private static final Set<String> SAFE_INLINE_TYPES =
            Set.of("image/png", "image/jpeg", "image/gif", "image/webp", "application/pdf", "text/plain");

    @GetMapping("/{attachmentId}/data")
    public ResponseEntity<byte[]> download(@PathVariable String slug, @PathVariable UUID attachmentId) {
        currentUserService.requireApproved();
        PageAttachment attachment = attachmentService.getAttachment(slug, attachmentId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(attachment.getContentType()));
        boolean safeInline = SAFE_INLINE_TYPES.contains(attachment.getContentType());
        headers.setContentDisposition((safeInline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(attachment.getFilename())
                .build());
        headers.set("X-Content-Type-Options", "nosniff");
        return new ResponseEntity<>(attachment.getData(), headers, HttpStatus.OK);
    }

    @DeleteMapping("/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String slug, @PathVariable UUID attachmentId) {
        currentUserService.requireApproved();
        attachmentService.deleteAttachment(slug, attachmentId);
    }
}
