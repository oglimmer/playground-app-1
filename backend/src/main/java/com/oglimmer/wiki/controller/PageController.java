package com.oglimmer.wiki.controller;

import com.oglimmer.wiki.dto.PageDto;
import com.oglimmer.wiki.dto.PageSummaryDto;
import com.oglimmer.wiki.dto.SavePageRequest;
import com.oglimmer.wiki.entity.AppUser;
import com.oglimmer.wiki.service.CurrentUserService;
import com.oglimmer.wiki.service.PageService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wiki CRUD. Every method requires an APPROVED user — enforced via {@link CurrentUserService}
 * rather than a URL role so an admin approval takes effect without a re-login.
 */
@RestController
@RequestMapping("/pages")
@RequiredArgsConstructor
public class PageController {

    private final PageService pageService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<PageSummaryDto> list(@RequestParam(required = false) String tag) {
        currentUserService.requireApproved();
        return (tag == null || tag.isBlank())
                ? pageService.list()
                : pageService.listByTag(tag.trim().toLowerCase());
    }

    @GetMapping("/tags")
    public List<String> tags() {
        currentUserService.requireApproved();
        return pageService.listTags();
    }

    @GetMapping("/{slug}")
    public PageDto get(@PathVariable String slug) {
        currentUserService.requireApproved();
        return pageService.get(slug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PageDto create(@Valid @RequestBody SavePageRequest request) {
        AppUser user = currentUserService.requireApproved();
        return pageService.create(request, user.getDisplayName());
    }

    @PutMapping("/{slug}")
    public PageDto update(@PathVariable String slug, @Valid @RequestBody SavePageRequest request) {
        AppUser user = currentUserService.requireApproved();
        return pageService.update(slug, request, user.getDisplayName());
    }

    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String slug) {
        currentUserService.requireApproved();
        pageService.delete(slug);
    }
}
