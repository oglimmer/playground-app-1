package com.oglimmer.wiki.repository;

import com.oglimmer.wiki.entity.Page;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PageRepository extends JpaRepository<Page, UUID> {
    Optional<Page> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Page> findAllByOrderByTitleAsc();

    List<Page> findAllByTagsContainingOrderByTitleAsc(String tag);

    @Query("select distinct t from Page p join p.tags t order by t")
    List<String> findDistinctTags();
}
