package com.ordermgmt.railway.ui.component;

import java.util.Locale;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.select.Select;

public class LanguageSwitcher extends Select<Locale> {

    private static final Locale DEFAULT_LOCALE = Locale.GERMAN;
    private static final Locale[] SUPPORTED_LOCALES = {
        Locale.GERMAN, Locale.ENGLISH, Locale.ITALIAN, Locale.FRENCH
    };

    public LanguageSwitcher() {
        setItems(SUPPORTED_LOCALES);
        setItemLabelGenerator(LanguageSwitcher::labelFor);

        Locale currentLocale = UI.getCurrent().getLocale();
        setValue(currentLocale != null ? currentLocale : DEFAULT_LOCALE);

        addValueChangeListener(
                event -> {
                    Locale selectedLocale = event.getValue();
                    if (selectedLocale == null) {
                        return;
                    }
                    UI.getCurrent().setLocale(selectedLocale);
                });

        setWidth("140px");
    }

    private static String labelFor(Locale locale) {
        return switch (locale.getLanguage()) {
            case "de" -> "Deutsch";
            case "en" -> "English";
            case "it" -> "Italiano";
            case "fr" -> "Français";
            default -> locale.getDisplayLanguage(locale);
        };
    }
}
