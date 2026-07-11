package com.oglimmer.wiki.security;

import com.oglimmer.wiki.entity.AppUser;
import com.oglimmer.wiki.entity.Role;
import com.oglimmer.wiki.service.UserProvisioningService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Bridges a Keycloak OIDC login to our local {@link AppUser}: provisions the account on first
 * login and grants Spring authorities based on the stored role. Approval status is intentionally
 * NOT encoded as an authority — it is checked live from the database on each wiki request so that
 * an admin approval takes effect without the user re-authenticating.
 */
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserProvisioningService provisioningService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String subject = oidcUser.getSubject();
        String email = oidcUser.getEmail() != null ? oidcUser.getEmail() : subject;
        String displayName = resolveDisplayName(oidcUser, email);

        AppUser user = provisioningService.provision(subject, email, displayName);

        List<GrantedAuthority> authorities = new ArrayList<>(oidcUser.getAuthorities());
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (user.getRole() == Role.ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        String nameAttributeKey = userRequest
                .getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();
        if (!StringUtils.hasText(nameAttributeKey)) {
            nameAttributeKey = "sub";
        }
        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), nameAttributeKey);
    }

    private String resolveDisplayName(OidcUser oidcUser, String fallback) {
        if (StringUtils.hasText(oidcUser.getPreferredUsername())) {
            return oidcUser.getPreferredUsername();
        }
        if (StringUtils.hasText(oidcUser.getFullName())) {
            return oidcUser.getFullName();
        }
        return fallback;
    }
}
