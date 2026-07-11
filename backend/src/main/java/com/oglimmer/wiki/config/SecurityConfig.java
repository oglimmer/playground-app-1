package com.oglimmer.wiki.config;

import com.oglimmer.wiki.security.CsrfCookieFilter;
import com.oglimmer.wiki.security.CustomOidcUserService;
import com.oglimmer.wiki.security.SpaCsrfTokenRequestHandler;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOidcUserService customOidcUserService;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookiePath("/");

        http.authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/actuator/health", "/actuator/health/**", "/actuator/info", "/error")
                        .permitAll()
                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .csrf(csrf ->
                        csrf.csrfTokenRepository(csrfRepo).csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .oauth2Login(
                        oauth -> oauth.userInfoEndpoint(userInfo -> userInfo.oidcUserService(customOidcUserService))
                                .successHandler(loginSuccessHandler()))
                .logout(logout -> logout.logoutUrl("/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(SecurityConfig::writeForbidden))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * Browser XHR (SPA fetch, {@code X-Requested-With: XMLHttpRequest}) gets a clean 401 so the
     * SPA can show its login screen; a plain top-level browser navigation is redirected straight
     * into the OIDC login flow. A single {@code authenticationEntryPoint(...)} must own both cases
     * — mixing it with {@code defaultAuthenticationEntryPointFor(...)} silently overrides the
     * per-request mappings.
     */
    private AuthenticationEntryPoint authenticationEntryPoint() {
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> mappings = new LinkedHashMap<>();
        mappings.put(
                new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest"),
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
        DelegatingAuthenticationEntryPoint entryPoint = new DelegatingAuthenticationEntryPoint(mappings);
        entryPoint.setDefaultEntryPoint((request, response, ex) ->
                response.sendRedirect(request.getContextPath() + "/oauth2/authorization/keycloak"));
        return entryPoint;
    }

    /**
     * After OIDC login, send the browser to the SPA root ("/") on the same origin — NOT the API.
     * A raw {@code sendRedirect("/")} (leading slash = container root per the Servlet spec) avoids
     * Spring's {@code DefaultRedirectStrategy}, which would prepend the {@code /api} context-path
     * and bounce the browser back into the backend. In prod Traefik routes "/" to the frontend and
     * "/api" to the backend on one host; in dev the Vite proxy does the same — either way the SPA
     * lives at "/".
     */
    private AuthenticationSuccessHandler loginSuccessHandler() {
        return (request, response, authentication) -> response.sendRedirect("/");
    }

    /** Emits the {@code {"error": "..."}} contract for authorization/CSRF failures. */
    private static void writeForbidden(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            org.springframework.security.access.AccessDeniedException ex)
            throws java.io.IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"Access denied\"}");
    }
}
