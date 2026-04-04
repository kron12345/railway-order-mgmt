package com.ordermgmt.railway.ui.util;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

import com.vaadin.flow.component.checkbox.CheckboxGroup;

import com.ordermgmt.railway.domain.infrastructure.model.PredefinedTag;

/**
 * Reusable tag selection logic for CheckboxGroup-based tag pickers. Extracted from duplicated
 * implementations in TimetableBuilderView, OrderFormPanel and ServicePositionDialog.
 *
 * <p>Usage: construct once per form, then call {@link #readTags(String)} to populate and {@link
 * #joinSelectedTags()} to serialize.
 */
public class TagSelectionHelper {

    private final CheckboxGroup<PredefinedTag> tagSelector;
    private final List<PredefinedTag> availableTags;
    private final LinkedHashSet<String> unmatchedTags;
    private final BiFunction<String, Object[], String> translator;

    public TagSelectionHelper(
            CheckboxGroup<PredefinedTag> tagSelector,
            List<PredefinedTag> availableTags,
            LinkedHashSet<String> unmatchedTags,
            BiFunction<String, Object[], String> translator) {
        this.tagSelector = tagSelector;
        this.availableTags = availableTags;
        this.unmatchedTags = unmatchedTags;
        this.translator = translator;
    }

    /**
     * Parses a stored comma-separated tag string, selects matching predefined tags in the checkbox
     * group, and collects unmatched legacy tags.
     */
    public void readTags(String stored) {
        Map<String, PredefinedTag> byName = new LinkedHashMap<>();
        availableTags.forEach(t -> byName.put(normalizeTagName(t.getName()), t));
        unmatchedTags.clear();
        LinkedHashSet<PredefinedTag> selected = new LinkedHashSet<>();
        for (String token : StringUtils.splitTags(stored)) {
            PredefinedTag match = byName.get(normalizeTagName(token));
            if (match != null) {
                selected.add(match);
            } else {
                unmatchedTags.add(token);
            }
        }
        tagSelector.setValue(selected);
    }

    /**
     * Returns a comma-separated string of all selected tag names (preserving catalog order) plus
     * any unmatched legacy tags. Returns {@code null} if empty.
     */
    public String joinSelectedTags() {
        LinkedHashSet<String> vals = new LinkedHashSet<>();
        for (PredefinedTag tag : availableTags) {
            if (tagSelector.getValue().contains(tag)) {
                vals.add(tag.getName());
            }
        }
        vals.addAll(unmatchedTags);
        return vals.isEmpty() ? null : String.join(", ", vals);
    }

    /**
     * Builds a helper text string indicating legacy (unmatched) tags when present.
     *
     * @param baseHelperKey i18n key for the base helper text
     * @param legacyKey i18n key for the legacy suffix (receives the joined legacy tag names)
     */
    public void updateHelperText(String baseHelperKey, String legacyKey) {
        String helper = translator.apply(baseHelperKey, new Object[0]);
        if (!unmatchedTags.isEmpty()) {
            helper +=
                    " "
                            + translator.apply(
                                    legacyKey, new Object[] {String.join(", ", unmatchedTags)});
        }
        tagSelector.setHelperText(helper);
    }

    /** Renders a tag label in the format {@code [Category] Name}. */
    public String tagLabel(PredefinedTag tag) {
        return "[" + tagCategoryLabel(tag.getCategory()) + "] " + tag.getName();
    }

    private String tagCategoryLabel(String category) {
        String n = category == null ? "general" : category.trim().toLowerCase(Locale.ROOT);
        return switch (n) {
            case "order" -> translator.apply("settings.tags.cat.order", new Object[0]);
            case "position" -> translator.apply("settings.tags.cat.position", new Object[0]);
            default -> translator.apply("settings.tags.cat.general", new Object[0]);
        };
    }

    private String normalizeTagName(String v) {
        return v == null ? "" : v.trim().toLowerCase(Locale.ROOT);
    }
}
