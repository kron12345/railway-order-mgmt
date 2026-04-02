package com.ordermgmt.railway.ui.theme;

import java.util.Set;

import com.vaadin.flow.component.UI;

/** Applies the persisted UI theme to the current browser document. */
public final class UiThemeUtil {

    public static final String DEFAULT_THEME = "dark-amber";
    private static final Set<String> SUPPORTED_THEMES = Set.of(DEFAULT_THEME, "dark-teal", "light");

    private UiThemeUtil() {}

    public static String normalize(String themeName) {
        if (themeName == null || themeName.isBlank()) {
            return DEFAULT_THEME;
        }
        return SUPPORTED_THEMES.contains(themeName) ? themeName : DEFAULT_THEME;
    }

    public static void applyToCurrentUi(String themeName) {
        apply(UI.getCurrent(), themeName);
    }

    public static void apply(UI ui, String themeName) {
        if (ui == null) {
            return;
        }

        String normalizedTheme = normalize(themeName);
        boolean darkTheme = !"light".equals(normalizedTheme);

        ui.getPage()
                .executeJs(
                        """
                        const root = document.documentElement;
                        root.setAttribute('data-rom-theme', $0);
                        const tokens = new Set((root.getAttribute('theme') || '').split(/\\s+/).filter(Boolean));
                        if ($1) {
                          tokens.add('dark');
                        } else {
                          tokens.delete('dark');
                        }
                        if (tokens.size > 0) {
                          root.setAttribute('theme', Array.from(tokens).join(' '));
                        } else {
                          root.removeAttribute('theme');
                        }
                        document.body?.setAttribute('data-rom-theme', $0);
                        """,
                        normalizedTheme,
                        darkTheme);
    }
}
