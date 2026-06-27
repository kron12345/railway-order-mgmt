package com.ordermgmt.railway.ui.component.business;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;

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

    private static final String USER_ICON = "👤 ";
    private static final String GROUP_ICON = "👥 ";
    private static final String ITEM_KEY_SEPARATOR = ":";

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
        setRenderer(new ComponentRenderer<>(this::renderItem));

        setItems(
                query -> {
                    String filterText = query.getFilter().orElse("").trim();
                    return search(filterText).stream()
                            .skip(query.getOffset())
                            .limit(query.getLimit());
                });

        addValueChangeListener(
                e -> {
                    // Ignore programmatic changes (e.g. preset() during card render) — only act
                    // on user-initiated selections, otherwise every render would persist again.
                    if (!e.isFromClient()) {
                        return;
                    }
                    Item selectedItem = e.getValue();
                    if (selectedItem == null) {
                        onChange.accept(null, null);
                    } else {
                        onChange.accept(selectedItem.type(), selectedItem.value());
                    }
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
        String label = type == AssignmentType.USER ? USER_ICON + value : GROUP_ICON + value;
        this.currentItem = new Item(type, value, label);
        setValue(this.currentItem);
    }

    private List<Item> search(String filter) {
        List<Item> items = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        // Always surface the current selection so it stays visible regardless of the
        // Keycloak filter — Vaadin needs the value to be present in the data provider.
        if (currentItem != null && matchesFilter(currentItem, filter)) {
            items.add(currentItem);
            seenKeys.add(key(currentItem.type(), currentItem.value()));
        }
        if (keycloakUserService == null) {
            return items;
        }
        try {
            String queryText = filter == null ? "" : filter;
            addUsers(items, seenKeys, keycloakUserService.searchUsers(queryText));
            addGroups(items, seenKeys, keycloakUserService.searchGroups(queryText));
            items.sort(
                    (left, right) ->
                            left.label()
                                    .toLowerCase(Locale.ROOT)
                                    .compareTo(right.label().toLowerCase(Locale.ROOT)));
        } catch (RuntimeException ex) {
            // Keycloak unreachable in tests / dev: degrade gracefully.
        }
        return items;
    }

    private static void addUsers(
            List<Item> items, Set<String> seenKeys, List<Map<String, String>> users) {
        for (Map<String, String> user : users) {
            String username = user.getOrDefault("username", "");
            if (username.isBlank() || !seenKeys.add(key(AssignmentType.USER, username))) {
                continue;
            }
            String fullName =
                    (user.getOrDefault("firstName", "") + " " + user.getOrDefault("lastName", ""))
                            .trim();
            String label = USER_ICON + username + (fullName.isBlank() ? "" : "  ·  " + fullName);
            items.add(new Item(AssignmentType.USER, username, label));
        }
    }

    private static void addGroups(
            List<Item> items, Set<String> seenKeys, List<Map<String, String>> groups) {
        for (Map<String, String> group : groups) {
            String name = group.getOrDefault("name", group.getOrDefault("path", ""));
            if (name.isBlank() || !seenKeys.add(key(AssignmentType.GROUP, name))) {
                continue;
            }
            items.add(new Item(AssignmentType.GROUP, name, GROUP_ICON + name));
        }
    }

    private static String key(AssignmentType type, String value) {
        return type + ITEM_KEY_SEPARATOR + value;
    }

    private static boolean matchesFilter(Item item, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        return item.value().toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
    }

    private Span renderItem(Item item) {
        var span = new Span(item.label());
        span.addClassName("assignee-item");
        span.addClassName("assignee-item--" + item.type().name().toLowerCase(Locale.ROOT));
        return span;
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
