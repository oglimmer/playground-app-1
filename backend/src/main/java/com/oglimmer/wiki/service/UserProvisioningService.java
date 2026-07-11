package com.oglimmer.wiki.service;

import com.oglimmer.wiki.entity.AppUser;
import com.oglimmer.wiki.entity.Role;
import com.oglimmer.wiki.entity.UserStatus;
import com.oglimmer.wiki.repository.AppUserRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private final AppUserRepository userRepository;

    /**
     * Loads the {@link AppUser} for an OIDC subject, creating one on first sight. The very first
     * user to ever log in is provisioned as an approved admin; everyone else starts as a pending
     * regular user awaiting admin approval.
     */
    @Transactional
    public AppUser provision(String subject, String email, String displayName) {
        return userRepository
                .findBySubject(subject)
                .map(existing -> refresh(existing, email, displayName))
                .orElseGet(() -> create(subject, email, displayName));
    }

    private AppUser refresh(AppUser user, String email, String displayName) {
        user.setEmail(email);
        user.setDisplayName(displayName);
        return userRepository.save(user);
    }

    private AppUser create(String subject, String email, String displayName) {
        boolean first = userRepository.count() == 0;
        AppUser user = new AppUser(
                UUID.randomUUID(),
                subject,
                email,
                displayName,
                first ? Role.ADMIN : Role.USER,
                first ? UserStatus.APPROVED : UserStatus.PENDING,
                Instant.now());
        AppUser saved = userRepository.save(user);
        log.info(
                "Provisioned user subject={} email={} as role={} status={}",
                subject,
                email,
                saved.getRole(),
                saved.getStatus());
        return saved;
    }
}
