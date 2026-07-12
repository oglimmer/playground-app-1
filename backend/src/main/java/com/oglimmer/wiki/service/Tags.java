package com.oglimmer.wiki.service;

import com.oglimmer.wiki.exception.ValidationException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Normalizes and validates tag lists: lowercased, deduplicated, URL-safe. */
public final class Tags {

    private static final Pattern VALID_TAG = Pattern.compile("^[a-z0-9_-]+$");

    private Tags() {}

    public static Set<String> normalize(Collection<String> rawTags) {
        Set<String> result = new LinkedHashSet<>();
        for (String raw : rawTags) {
            if (raw == null) continue;
            String trimmed = raw.trim().toLowerCase(Locale.ROOT);
            if (trimmed.isEmpty()) continue;
            if (!VALID_TAG.matcher(trimmed).matches()) {
                throw new ValidationException("Invalid tag: " + raw);
            }
            result.add(trimmed);
        }
        return result;
    }
}
