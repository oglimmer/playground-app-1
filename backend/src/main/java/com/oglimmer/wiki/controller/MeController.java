package com.oglimmer.wiki.controller;

import com.oglimmer.wiki.dto.UserDto;
import com.oglimmer.wiki.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Session identity endpoint. Returns 401 for anonymous callers (handled by Spring Security), so a
 * 200 here always carries an authenticated user's profile — including their approval status, which
 * the SPA uses to decide between the wiki and the "waiting for approval" screen.
 */
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final CurrentUserService currentUserService;

    @GetMapping
    public UserDto me() {
        return UserDto.from(currentUserService.current());
    }
}
