package com.ordermgmt.railway.ui.component.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;

import com.ordermgmt.railway.domain.business.model.AssignmentType;
import com.ordermgmt.railway.infrastructure.keycloak.KeycloakUserService;

/**
 * ComboBox that lets the user pick either a Keycloak <i>user</i> or <i>group</i> as assignee. Items
 * are uniformly modeled as {@link Item} records and rendered with a leading {@code [Person] /
 * [Team]} tag so the kind is unambiguous.
 *
 * <p>Server-side filtering: typing in the input issues a paged Keycloak search; combo shows the
 * merged result list. Selection invokes the supplied callback with both the type discriminator and
 * the canonical name (preferred-username for users, group-path for groups). Pass {@code (null,
 * null)} from outside to clear.
 */
public class AssigneeComboBox extends ComboBox<AssigneeComboBox.Item> {

    public record Item(AssignmentType type, String value, String label) {}

    private final KeycloakUserService keycloakUserService;
    private final BiConsumer<AssignmentType, String> onChange;

    /**
     * Currently selected item; surfaced from the lazy data callback so the preset value is visible
     * even before the dropdown queries Keycloak.
     */
    private Item currentItem;

    public AssigneeComboBox(
            KeycloakUserService keycloakUserService, BiConsumer<AssignmentType, String> onChange) {
        this.keycloakUserService = keycloakUserService;
        this.onChange = onChange;

        addThemeVariants(ComboBoxVariant.LUMO_SMALL);
        setClearButtonVisible(true);
        setItemLabelGenerator(Item::label);
        setRenderer(new com.vaadin.flow.data.renderer.ComponentRenderer<>(this::renderItem));

        // Keycloak does the filtering server-side; we treat the typed text as the query.
        setItems(
                query -> {
                    String filter = query.getFilter().orElse("").trim();
                    return search(filter).stream().skip(query.getOffset()).limit(query.getLimit());
                });

        addValueChangeListener(
                e -> {
                    // Ignore programmatic changes (e.g. preset() during card render) — only act
                    // on user-initiated selections, otherwise every render would persist again.
                    if (!e.isFromClient()) return;
                    Item v = e.getValue();
                    if (v == null) onChange.accept(null, null);
                    else onChange.accept(v.type(), v.value());
                });
    }

    /**
     * Pre-select an item by type+value. Stores the {@link #currentItem} so the lazy data callback
     * surfaces it on first dropdown open — does <em>not</em> replace the data provider, so the user
     * can still search and reassign.
     */
    public void preset(AssignmentType type, String value) {
        if (type == null || value == null || value.isBlank()) {
            this.currentItem = null;
            setValue(null);
            return;
        }
        String label = type == AssignmentType.USER ? "👤 " + value : "👥 " + value;
        this.currentItem = new Item(type, value, label);
        setValue(this.currentItem);
    }

    private List<Item> search(String filter) {
        List<Item> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        // Always surface the current selection so it stays visible regardless of the
        // Keycloak filter — Vaadin needs the value to be present in the data provider.
        if (currentItem != null && matchesFilter(currentItem, filter)) {
            result.add(currentItem);
            seen.add(currentItem.type() + ":" + currentItem.value());
        }
        if (keycloakUserService == null) return result;
        try {
            String q = filter == null ? "" : filter;
            List<Map<String, String>> users = keycloakUserService.searchUsers(q);
            for (Map<String, String> u : users) {
                String username = u.getOrDefault("username", "");
                if (username.isBlank()) continue;
                if (!seen.add(AssignmentType.USER + ":" + username)) continue;
                String full =
                        (u.getOrDefault("firstName", "") + " " + u.getOrDefault("lastName", ""))
                                .trim();
                String label = "👤 " + username + (full.isBlank() ? "" : "  ·  " + full);
                result.add(new Item(AssignmentType.USER, username, label));
            }
            List<Map<String, String>> groups = keycloakUserService.searchGroups(q);
            for (Map<String, String> g : groups) {
                String name = g.getOrDefault("name", g.getOrDefault("path", ""));
                if (name.isBlank()) continue;
                if (!seen.add(AssignmentType.GROUP + ":" + name)) continue;
                result.add(new Item(AssignmentType.GROUP, name, "👥 " + name));
            }
            // Sort by label, with users typically appearing first.
            result.sort(
                    (a, b) ->
                            a.label()
                                    .toLowerCase(Locale.ROOT)
                                    .compareTo(b.label().toLowerCase(Locale.ROOT)));
        } catch (RuntimeException ex) {
            // Keycloak unreachable in tests / dev: degrade gracefully.
        }
        return result;
    }

    private static boolean matchesFilter(Item item, String filter) {
        if (filter == null || filter.isBlank()) return true;
        return item.value().toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
    }

    private com.vaadin.flow.component.html.Span renderItem(Item it) {
        var s = new com.vaadin.flow.component.html.Span(it.label());
        s.addClassName("assignee-item");
        s.addClassName("assignee-item--" + it.type().name().toLowerCase());
        return s;
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public int hashCode() {
        // Identity hash, consistent with the identity-based equals above. NOTE: must NOT be
        // Objects.hashCode(this) — that calls this.hashCode() and recurses into a
        // StackOverflowError.
        return System.identityHashCode(this);
    }
}
