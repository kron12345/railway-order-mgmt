package com.ordermgmt.railway.infrastructure.i18n;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.vaadin.flow.i18n.I18NProvider;

@Component
public class TranslationProvider implements I18NProvider {

    private static final Logger log = LoggerFactory.getLogger(TranslationProvider.class);
    private static final String BUNDLE_PREFIX = "i18n.messages";

    public static final Locale GERMAN = Locale.GERMAN;
    public static final Locale ENGLISH = Locale.ENGLISH;
    public static final Locale ITALIAN = Locale.ITALIAN;
    public static final Locale FRENCH = Locale.FRENCH;

    @Override
    public List<Locale> getProvidedLocales() {
        return List.of(GERMAN, ENGLISH, ITALIAN, FRENCH);
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        if (key == null) {
            log.warn("Translation key is null");
            return "";
        }

        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_PREFIX, locale);
            String value = bundle.getString(key);
            if (params.length > 0) {
                value = MessageFormat.format(value, params);
            }
            return value;
        } catch (MissingResourceException e) {
            log.warn("Missing translation: key='{}', locale='{}'", key, locale);
            return "!" + key + "!";
        }
    }
}
