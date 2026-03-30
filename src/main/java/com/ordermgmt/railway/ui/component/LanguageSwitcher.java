package com.ordermgmt.railway.ui.component;

import java.util.Locale;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.select.Select;

/** Locale selector for switching the current UI language. */
public class LanguageSwitcher extends Select<Locale> {

    public LanguageSwitcher() {
        setItems(Locale.GERMAN, Locale.ENGLISH, Locale.ITALIAN, Locale.FRENCH);
        setItemLabelGenerator(
                locale ->
                        switch (locale.getLanguage()) {
                            case "de" -> "Deutsch";
                            case "en" -> "English";
                            case "it" -> "Italiano";
                            case "fr" -> "Français";
                            default -> locale.getDisplayLanguage(locale);
                        });

        Locale currentLocale = UI.getCurrent().getLocale();
        setValue(currentLocale != null ? currentLocale : Locale.GERMAN);

        addValueChangeListener(
                event -> {
                    if (event.getValue() != null) {
                        UI.getCurrent().setLocale(event.getValue());
                    }
                });

        setWidth("140px");
    }
}
