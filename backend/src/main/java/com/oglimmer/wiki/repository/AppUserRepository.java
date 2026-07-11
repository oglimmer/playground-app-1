package com.oglimmer.wiki.repository;

import com.oglimmer.wiki.entity.AppUser;
import com.oglimmer.wiki.entity.UserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findBySubject(String subject);

    List<AppUser> findByStatusOrderByCreatedAtAsc(UserStatus status);

    List<AppUser> findAllByOrderByCreatedAtAsc();
}
