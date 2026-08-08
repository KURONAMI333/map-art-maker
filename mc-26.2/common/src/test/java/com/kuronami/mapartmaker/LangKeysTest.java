package com.kuronami.mapartmaker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Every lang file must translate exactly the same set of keys. A key present in one file and
 * missing from another is invisible in normal play — the string just falls back to the key
 * itself — so this is worth catching mechanically rather than by eye.
 */
class LangKeysTest {

    private static final String[] LOCALES = {
            "de_de", "en_us", "es_es", "fr_fr", "ja_jp", "ko_kr", "pt_br", "ru_ru", "zh_cn"
    };

    @Test
    void everyLocaleTranslatesTheSameKeys() {
        Map<String, Set<String>> keysByLocale = new java.util.LinkedHashMap<>();
        for (String locale : LOCALES) {
            keysByLocale.put(locale, readKeys(locale));
        }

        Set<String> reference = keysByLocale.get("en_us");
        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : keysByLocale.entrySet()) {
            Set<String> missing = new TreeSet<>(reference);
            missing.removeAll(entry.getValue());
            Set<String> extra = new TreeSet<>(entry.getValue());
            extra.removeAll(reference);
            if (!missing.isEmpty() || !extra.isEmpty()) {
                mismatches.add(entry.getKey() + ": missing " + missing + ", extra " + extra);
            }
        }
        assertTrue(mismatches.isEmpty(), () -> "lang key sets diverge:\n" + String.join("\n", mismatches));
    }

    private static Set<String> readKeys(String locale) {
        String path = "assets/map_art_maker/lang/" + locale + ".json";
        try (InputStream in = LangKeysTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                fail("lang file not on the test classpath: " + path);
            }
            JsonElement root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            JsonObject object = root.getAsJsonObject();
            Set<String> keys = new TreeSet<>();
            object.keySet().forEach(keys::add);
            return keys;
        } catch (IOException e) {
            fail("could not read " + path + ": " + e.getMessage());
            throw new AssertionError("unreachable");
        }
    }
}
