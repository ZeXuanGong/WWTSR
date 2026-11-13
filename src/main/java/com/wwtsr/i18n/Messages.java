package com.wwtsr.i18n;

import com.wwtsr.settings.Language;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/** The app's text in one language. */
public final class Messages {

    private static final String BUNDLE = "com.wwtsr.i18n.messages";

    private final ResourceBundle bundle;

    private Messages(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    public static Messages forLanguage(Language language) {
        return new Messages(ResourceBundle.getBundle(BUNDLE, localeFor(language, Locale.getDefault())));
    }

    /** English is the root bundle, so it is asked for with the root locale. */
    static Locale localeFor(Language language, Locale system) {
        return switch (language) {
            case CHINESE -> Locale.SIMPLIFIED_CHINESE;
            case ENGLISH -> Locale.ROOT;
            case AUTO -> "zh".equals(system.getLanguage()) ? Locale.SIMPLIFIED_CHINESE : Locale.ROOT;
        };
    }

    /** The text for {@code key}; a key with no text shows as itself rather than failing. */
    public String get(String key, Object... args) {
        try {
            String text = bundle.getString(key);
            return args.length == 0 ? text : MessageFormat.format(text, args);
        } catch (MissingResourceException | IllegalArgumentException e) {
            return key;
        }
    }
}
