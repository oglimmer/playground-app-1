package com.oglimmer.wiki.controller;

import com.oglimmer.wiki.dto.UserDto;
import com.oglimmer.wiki.service.AdminService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only user management. Access is gated to ROLE_ADMIN in {@code SecurityConfig}. */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    public List<UserDto> list() {
        return adminService.listUsers();
    }

    @PostMapping("/{id}/approve")
    public UserDto approve(@PathVariable UUID id) {
        return adminService.approve(id);
    }

    @PostMapping("/{id}/revoke")
    public UserDto revoke(@PathVariable UUID id) {
        return adminService.revoke(id);
    }
}
