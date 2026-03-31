package com.ordermgmt.railway.ui.view.profile;

import java.util.List;
import java.util.Map;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.infrastructure.keycloak.CurrentUserHelper;
import com.ordermgmt.railway.infrastructure.keycloak.KeycloakUserService;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Personal profile and preferences — data stored in Keycloak user attributes. */
@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Profile")
@PermitAll
public class ProfileView extends VerticalLayout {

    private final KeycloakUserService keycloakService;
    private final String userId;
    private Map<String, String> attrs;

    private ComboBox<String> localeSelect;
    private ComboBox<String> themeSelect;
    private ComboBox<String> fpjSelect;
    private ComboBox<String> countrySelect;
    private ComboBox<String> timezoneSelect;

    public ProfileView(KeycloakUserService keycloakService) {
        this.keycloakService = keycloakService;
        this.userId = CurrentUserHelper.getUserId();

        setPadding(false);
        setWidthFull();
        getStyle()
                .set("background", "var(--rom-bg-primary)")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-m)")
                .set("overflow-x", "hidden")
                .set("box-sizing", "border-box");

        loadAndBuild();
    }

    private void loadAndBuild() {
        removeAll();
        attrs = keycloakService.getUserAttributes(userId);
        List<String> roles = keycloakService.getUserRoles(userId);

        add(createHeader());
        add(createProfileCard(roles));
        add(createPreferencesCard());
    }

    private H2 createHeader() {
        H2 title = new H2(getTranslation("profile.title"));
        title.getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("font-weight", "600")
                .set("margin", "0 0 var(--lumo-space-s) 0");
        return title;
    }

    private Div createProfileCard(List<String> roles) {
        Div card = card();
        H3 title = sectionTitle(getTranslation("profile.info"));
        card.add(title);

        HorizontalLayout info = new HorizontalLayout();
        info.setWidthFull();
        info.getStyle().set("gap", "24px").set("flex-wrap", "wrap");

        info.add(infoBox(getTranslation("profile.username"),
                attrs.getOrDefault("username", "—")));
        info.add(infoBox(getTranslation("profile.name"),
                attrs.getOrDefault("firstName", "") + " " + attrs.getOrDefault("lastName", "")));
        info.add(infoBox(getTranslation("profile.email"),
                attrs.getOrDefault("email", "—")));

        card.add(info);

        // Roles
        Div rolesSection = new Div();
        rolesSection.getStyle().set("margin-top", "var(--lumo-space-m)");
        Span rolesLabel = new Span(getTranslation("profile.roles"));
        rolesLabel.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "10px")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.06em")
                .set("color", "var(--rom-text-muted)")
                .set("display", "block")
                .set("margin-bottom", "var(--lumo-space-xs)");
        rolesSection.add(rolesLabel);

        HorizontalLayout roleBadges = new HorizontalLayout();
        roleBadges.getStyle().set("gap", "6px").set("flex-wrap", "wrap");
        for (String role : roles) {
            if (role.startsWith("default-roles-") || role.equals("offline_access")
                    || role.equals("uma_authorization")) {
                continue;
            }
            Span badge = new Span(role.toUpperCase());
            badge.getStyle()
                    .set("font-family", "'JetBrains Mono', monospace")
                    .set("font-size", "10px")
                    .set("font-weight", "600")
                    .set("padding", "2px 8px")
                    .set("border-radius", "4px")
                    .set("color", "var(--rom-accent)")
                    .set("background", "var(--rom-accent-muted)")
                    .set("border", "1px solid var(--rom-accent)");
            roleBadges.add(badge);
        }
        rolesSection.add(roleBadges);
        card.add(rolesSection);

        return card;
    }

    private Div createPreferencesCard() {
        Div card = card();
        H3 title = sectionTitle(getTranslation("profile.preferences"));
        card.add(title);

        localeSelect = new ComboBox<>(getTranslation("profile.language"));
        localeSelect.setItems("de", "en", "fr", "it");
        localeSelect.setItemLabelGenerator(this::localeName);
        localeSelect.setValue(attrs.getOrDefault("locale", "de"));
        localeSelect.setWidth("200px");

        themeSelect = new ComboBox<>(getTranslation("profile.theme"));
        themeSelect.setItems("dark-amber", "dark-teal", "light");
        themeSelect.setItemLabelGenerator(t -> switch (t) {
            case "dark-amber" -> "Dark (Amber)";
            case "dark-teal" -> "Dark (Teal)";
            case "light" -> "Light";
            default -> t;
        });
        themeSelect.setValue(attrs.getOrDefault("theme", "dark-amber"));
        themeSelect.setWidth("200px");

        fpjSelect = new ComboBox<>(getTranslation("profile.defaultFpj"));
        fpjSelect.setItems("FPJ 2025", "FPJ 2026", "FPJ 2027", "FPJ 2028");
        fpjSelect.setClearButtonVisible(true);
        fpjSelect.setValue(attrs.getOrDefault("defaultFpj", null));
        fpjSelect.setWidth("200px");

        countrySelect = new ComboBox<>(getTranslation("profile.defaultCountry"));
        countrySelect.setItems("CHE", "DEU");
        countrySelect.setItemLabelGenerator(c -> "CHE".equals(c) ? "Schweiz (CH)" : "Deutschland (DE)");
        countrySelect.setValue(attrs.getOrDefault("defaultCountry", null));
        countrySelect.setWidth("200px");

        timezoneSelect = new ComboBox<>(getTranslation("profile.timezone"));
        timezoneSelect.setItems("Europe/Zurich", "Europe/Berlin", "Europe/Vienna", "Europe/Paris", "UTC");
        timezoneSelect.setValue(attrs.getOrDefault("timezone", "Europe/Zurich"));
        timezoneSelect.setWidth("200px");

        Div row1 = new Div(localeSelect, themeSelect, timezoneSelect);
        row1.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "var(--lumo-space-s)")
                .set("margin-bottom", "var(--lumo-space-s)");

        Div row2 = new Div(fpjSelect, countrySelect);
        row2.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "var(--lumo-space-s)")
                .set("margin-bottom", "var(--lumo-space-s)");

        Button save = new Button(getTranslation("common.save"), VaadinIcon.CHECK.create());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)")
                .set("font-weight", "600");
        save.addClickListener(e -> savePreferences());

        card.add(row1, row2, save);
        return card;
    }

    private void savePreferences() {
        Map<String, String> prefs = Map.of(
                "locale", localeSelect.getValue() != null ? localeSelect.getValue() : "de",
                "theme", themeSelect.getValue() != null ? themeSelect.getValue() : "dark-amber",
                "defaultFpj", fpjSelect.getValue() != null ? fpjSelect.getValue() : "",
                "defaultCountry", countrySelect.getValue() != null ? countrySelect.getValue() : "",
                "timezone", timezoneSelect.getValue() != null ? timezoneSelect.getValue() : "Europe/Zurich");

        boolean ok = keycloakService.updateUserAttributes(userId, prefs);
        if (ok) {
            Notification.show(getTranslation("common.save") + " ✓", 3000,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Apply locale change immediately
            String locale = prefs.get("locale");
            if (locale != null && !locale.isBlank()) {
                UI.getCurrent().setLocale(java.util.Locale.forLanguageTag(locale));
            }
        } else {
            Notification.show(getTranslation("settings.import.error"), 3000,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private String localeName(String code) {
        return switch (code) {
            case "de" -> "Deutsch";
            case "en" -> "English";
            case "fr" -> "Français";
            case "it" -> "Italiano";
            default -> code;
        };
    }

    private Div infoBox(String label, String value) {
        Div box = new Div();
        box.getStyle()
                .set("background", "var(--rom-bg-primary)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "10px 14px")
                .set("min-width", "160px");

        Span val = new Span(value);
        val.getStyle()
                .set("font-size", "14px")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-primary)")
                .set("display", "block");

        Span lbl = new Span(label);
        lbl.getStyle()
                .set("font-size", "10px")
                .set("color", "var(--rom-text-muted)")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.06em")
                .set("display", "block");

        box.add(val, lbl);
        return box;
    }

    private Div card() {
        Div div = new Div();
        div.setWidthFull();
        div.getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "var(--lumo-space-m) var(--lumo-space-l)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("box-sizing", "border-box");
        return div;
    }

    private H3 sectionTitle(String text) {
        H3 h = new H3(text);
        h.getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("margin", "0 0 var(--lumo-space-s) 0")
                .set("font-size", "var(--lumo-font-size-m)");
        return h;
    }
}
