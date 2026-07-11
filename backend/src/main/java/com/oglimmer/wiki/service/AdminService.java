package com.oglimmer.wiki.service;

import com.oglimmer.wiki.dto.UserDto;
import com.oglimmer.wiki.entity.AppUser;
import com.oglimmer.wiki.entity.UserStatus;
import com.oglimmer.wiki.exception.NotFoundException;
import com.oglimmer.wiki.repository.AppUserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AppUserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserDto> listUsers() {
        return userRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(UserDto::from)
                .toList();
    }

    @Transactional
    public UserDto approve(UUID userId) {
        AppUser user = require(userId);
        user.setStatus(UserStatus.APPROVED);
        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    public UserDto revoke(UUID userId) {
        AppUser user = require(userId);
        user.setStatus(UserStatus.PENDING);
        return UserDto.from(userRepository.save(user));
    }

    private AppUser require(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }
}
