package com.oglimmer.wiki;

import static org.assertj.core.api.Assertions.assertThat;

import com.oglimmer.wiki.service.Slugs;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.boot.test.context.SpringBootTest;
import org.yaml.snakeyaml.Yaml;

@SpringBootTest
class WikiApplicationTests {

    @Test
    void contextLoads() {}

    /**
     * Reads the production {@code application.yaml} directly (bypassing Spring's environment) so
     * that this assertion is unaffected by test-only property overrides such as
     * src/test/resources/application.yaml shadowing the main config on the test classpath.
     */
    @Test
    void sessionStaysAliveForAtLeastTwentyFourHours() throws IOException {
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(Path.of("src/main/resources/application.yaml"))) {
            root = new Yaml().load(in);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> server = (Map<String, Object>) root.get("server");
        @SuppressWarnings("unchecked")
        Map<String, Object> servlet = (Map<String, Object>) server.get("servlet");
        @SuppressWarnings("unchecked")
        Map<String, Object> session = (Map<String, Object>) servlet.get("session");
        @SuppressWarnings("unchecked")
        Map<String, Object> cookie = (Map<String, Object>) session.get("cookie");

        Duration timeout = DurationStyle.detectAndParse((String) session.get("timeout"));
        assertThat(timeout).isGreaterThanOrEqualTo(Duration.ofHours(24));

        Duration cookieMaxAge = DurationStyle.detectAndParse((String) cookie.get("max-age"));
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
