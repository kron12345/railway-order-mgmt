package com.ordermgmt.railway.ui.component.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;

import com.ordermgmt.railway.domain.business.model.AssignmentType;
import com.ordermgmt.railway.infrastructure.keycloak.KeycloakUserService;

/**
 * ComboBox that lets the user pick either a Keycloak <i>user</i> or <i>group</i> as
 * assignee. Items are uniformly modeled as {@link Item} records and rendered with a
 * leading {@code [Person] / [Team]} tag so the kind is unambiguous.
 *
 * <p>Server-side filtering: typing in the input issues a paged Keycloak search; combo
 * shows the merged result list. Selection invokes the supplied callback with both the
 * type discriminator and the canonical name (preferred-username for users, group-path
 * for groups). Pass {@code (null, null)} from outside to clear.
 */
public class AssigneeComboBox extends ComboBox<AssigneeComboBox.Item> {

    public record Item(AssignmentType type, String value, String label) {}

    private final KeycloakUserService keycloakUserService;
    private final BiConsumer<AssignmentType, String> onChange;

    public AssigneeComboBox(KeycloakUserService keycloakUserService,
                            BiConsumer<AssignmentType, String> onChange) {
        this.keycloakUserService = keycloakUserService;
        this.onChange = onChange;

        addThemeVariants(ComboBoxVariant.LUMO_SMALL);
        setClearButtonVisible(true);
        setItemLabelGenerator(Item::label);
        setRenderer(new com.vaadin.flow.data.renderer.ComponentRenderer<>(this::renderItem));

        // Keycloak does the filtering server-side; we treat the typed text as the query.
        setItems(query -> {
            String filter = query.getFilter().orElse("").trim();
            return search(filter).stream()
                    .skip(query.getOffset())
                    .limit(query.getLimit());
        });

        addValueChangeListener(e -> {
            Item v = e.getValue();
            if (v == null) onChange.accept(null, null);
            else onChange.accept(v.type(), v.value());
        });
    }

    /** Pre-select an item by type+value without triggering the change callback. */
    public void preset(AssignmentType type, String value) {
        if (type == null || value == null || value.isBlank()) {
            setValue(null);
            return;
        }
        // Build a synthetic item so the combo can render the current value even
        // before the user opens the dropdown.
        String label = type == AssignmentType.USER ? "👤 " + value : "👥 " + value;
        Item item = new Item(type, value, label);
        setItems(List.of(item));
        setValue(item);
    }

    private List<Item> search(String filter) {
        List<Item> result = new ArrayList<>();
        if (keycloakUserService == null) return result;
        try {
            String q = filter == null ? "" : filter;
            List<Map<String, String>> users = keycloakUserService.searchUsers(q);
            for (Map<String, String> u : users) {
                String username = u.getOrDefault("username", "");
                if (username.isBlank()) continue;
                String full = (u.getOrDefault("firstName", "") + " "
                        + u.getOrDefault("lastName", "")).trim();
                String label = "👤 " + username + (full.isBlank() ? "" : "  ·  " + full);
                result.add(new Item(AssignmentType.USER, username, label));
            }
            List<Map<String, String>> groups = keycloakUserService.searchGroups(q);
            for (Map<String, String> g : groups) {
                String name = g.getOrDefault("name", g.getOrDefault("path", ""));
                if (name.isBlank()) continue;
                result.add(new Item(AssignmentType.GROUP, name, "👥 " + name));
            }
            // Sort by label, with users typically appearing first.
            result.sort((a, b) -> a.label().toLowerCase(Locale.ROOT)
                    .compareTo(b.label().toLowerCase(Locale.ROOT)));
        } catch (RuntimeException ex) {
            // Keycloak unreachable in tests / dev: degrade gracefully to empty list.
        }
        return result;
    }

    private com.vaadin.flow.component.html.Span renderItem(Item it) {
        var s = new com.vaadin.flow.component.html.Span(it.label());
        s.addClassName("assignee-item");
        s.addClassName("assignee-item--" + it.type().name().toLowerCase());
        return s;
    }

    @Override
    public boolean equals(Object o) { return o == this; }
    @Override
    public int hashCode() { return Objects.hashCode(this); }
}
