package com.wwtsr.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.wwtsr.settings.Language;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class MessagesTest {

    private static Properties load(String name) throws IOException {
        Properties p = new Properties();
        try (InputStream in = Messages.class.getResourceAsStream(name)) {
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }

    @Test
    void everyTextExistsInBothLanguages() throws IOException {
        assertEquals(load("messages.properties").keySet(), load("messages_zh.properties").keySet());
    }

    @Test
    void eachLanguageGivesItsOwnText() {
        assertEquals("Quit", Messages.forLanguage(Language.ENGLISH).get("menu.quit"));
        assertEquals("退出应用", Messages.forLanguage(Language.CHINESE).get("menu.quit"));
        assertEquals("Hide pet (F9)", Messages.forLanguage(Language.ENGLISH).get("menu.withKey", "Hide pet", "F9"));
    }

    @Test
    void autoFollowsTheSystemLanguage() {
        assertEquals(Locale.SIMPLIFIED_CHINESE, Messages.localeFor(Language.AUTO, Locale.TRADITIONAL_CHINESE));
        assertEquals(Locale.ROOT, Messages.localeFor(Language.AUTO, Locale.FRANCE));
        assertEquals(Locale.ROOT, Messages.localeFor(Language.ENGLISH, Locale.SIMPLIFIED_CHINESE),
                "English must not fall back to the system's Chinese");
    }

    @Test
    void aMissingTextShowsItsKey() {
        assertEquals("no.such.key", Messages.forLanguage(Language.ENGLISH).get("no.such.key"));
    }
}
