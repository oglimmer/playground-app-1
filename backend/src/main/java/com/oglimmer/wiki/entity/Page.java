package com.oglimmer.wiki.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "page")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Page {

    @Id
    private UUID id;

    /** URL-safe unique identifier for the page. */
    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    /** Raw Markdown body. */
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Display name of the last editor. */
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    /** Normalized, URL-safe tags associated with this page. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "page_tag", joinColumns = @JoinColumn(name = "page_id"))
    @Column(name = "tag", nullable = false, length = 64)
    private Set<String> tags = new LinkedHashSet<>();
}
