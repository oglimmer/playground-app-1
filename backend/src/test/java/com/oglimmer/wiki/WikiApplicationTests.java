package com.oglimmer.wiki;

import static org.assertj.core.api.Assertions.assertThat;

import com.oglimmer.wiki.service.Slugs;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WikiApplicationTests {

    @Autowired
    private ServerProperties serverProperties;

    @Test
    void contextLoads() {}

    @Test
    void sessionStaysAliveForAtLeastTwentyFourHours() {
        Duration timeout = serverProperties.getServlet().getSession().getTimeout();
        assertThat(timeout).isNotNull();
        assertThat(timeout).isGreaterThanOrEqualTo(Duration.ofHours(24));

        Duration cookieMaxAge =
                serverProperties.getServlet().getSession().getCookie().getMaxAge();
        assertThat(cookieMaxAge).isNotNull();
        assertThat(cookieMaxAge).isGreaterThanOrEqualTo(Duration.ofHours(24));
    }

    @Test
    void slugifyProducesUrlSafeSlugs() {
        assertThat(Slugs.slugify("Hello, World!")).isEqualTo("hello-world");
        assertThat(Slugs.slugify("  Über Café  ")).isEqualTo("uber-cafe");
        assertThat(Slugs.slugify("***")).isEqualTo("page");
    }

    @Test
    void tagsNormalizeLowercasesDedupsAndValidates() {
        java.util.Set<String> tags =
                com.oglimmer.wiki.service.Tags.normalize(java.util.List.of("Alpha", "alpha", "b_2", "c-3", " "));
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Set.of("alpha", "b_2", "c-3"), tags);
    }

    @Test
    void tagsNormalizeRejectsInvalid() {
        org.junit.jupiter.api.Assertions.assertThrows(
                com.oglimmer.wiki.exception.ValidationException.class,
                () -> com.oglimmer.wiki.service.Tags.normalize(java.util.List.of("bad tag!")));
    }
}
