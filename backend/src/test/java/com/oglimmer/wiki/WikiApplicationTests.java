package com.oglimmer.wiki;

import static org.assertj.core.api.Assertions.assertThat;

import com.oglimmer.wiki.service.Slugs;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WikiApplicationTests {

    @Test
    void contextLoads() {}

    @Test
    void slugifyProducesUrlSafeSlugs() {
        assertThat(Slugs.slugify("Hello, World!")).isEqualTo("hello-world");
        assertThat(Slugs.slugify("  Über Café  ")).isEqualTo("uber-cafe");
        assertThat(Slugs.slugify("***")).isEqualTo("page");
    }
}
