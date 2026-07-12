package com.oglimmer.wiki.controller;

import com.oglimmer.wiki.service.CurrentUserService;
import com.oglimmer.wiki.service.PageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lists the distinct tags across all pages. Kept outside the page slug namespace. */
@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final PageService pageService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<String> tags() {
        currentUserService.requireApproved();
        return pageService.listTags();
    }
}
