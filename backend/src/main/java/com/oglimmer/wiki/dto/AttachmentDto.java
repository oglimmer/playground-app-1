package com.oglimmer.wiki.dto;

import com.oglimmer.wiki.entity.PageAttachment;
import java.util.UUID;

public record AttachmentDto(UUID id, String filename, String contentType, long size) {

    public static AttachmentDto from(PageAttachment attachment) {
        return new AttachmentDto(
                attachment.getId(), attachment.getFilename(), attachment.getContentType(), attachment.getSize());
    }
}
