package com.oglimmer.wiki.service;

import com.oglimmer.wiki.entity.AppUser;
import com.oglimmer.wiki.entity.UserStatus;
import com.oglimmer.wiki.exception.ForbiddenException;
import com.oglimmer.wiki.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final AppUserRepository userRepository;

    /** The authenticated user's local record, read fresh so approval changes apply immediately. */
    @Transactional(readOnly = true)
    public AppUser current() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            throw new ForbiddenException("Not authenticated");
        }
        return userRepository
                .findBySubject(oidcUser.getSubject())
                .orElseThrow(() -> new ForbiddenException("Unknown user"));
    }

    /** The current user, but only if an admin has approved them; otherwise 403. */
    @Transactional(readOnly = true)
    public AppUser requireApproved() {
        AppUser user = current();
        if (user.getStatus() != UserStatus.APPROVED) {
            throw new ForbiddenException("Your account is awaiting admin approval");
        }
        return user;
    }
}
