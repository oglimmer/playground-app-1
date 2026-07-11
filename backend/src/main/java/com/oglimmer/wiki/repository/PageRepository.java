package com.oglimmer.wiki.repository;

import com.oglimmer.wiki.entity.Page;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PageRepository extends JpaRepository<Page, UUID> {
    Optional<Page> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Page> findAllByOrderByTitleAsc();
}
