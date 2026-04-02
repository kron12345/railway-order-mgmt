package com.ordermgmt.railway.ui.component.timetable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.CheckboxGroup;

import com.ordermgmt.railway.domain.infrastructure.model.PredefinedTag;

/**
 * Tag selection and matching logic for the timetable builder. Extracted from {@link
 * TimetableBuilderView} to keep file sizes manageable.
 */
public class TimetableTagHelper {

    private final CheckboxGroup<PredefinedTag> tagSelector;
    private final List<PredefinedTag> availableTags;
    private final LinkedHashSet<String> unmatchedTags;
    private final Component translationSource;

    public TimetableTagHelper(
            CheckboxGroup<PredefinedTag> tagSelector,
            List<PredefinedTag> availableTags,
            LinkedHashSet<String> unmatchedTags,
            Component translationSource) {
        this.tagSelector = tagSelector;
        this.availableTags = availableTags;
        this.unmatchedTags = unmatchedTags;
        this.translationSource = translationSource;
    }

    public void readTags(String stored) {
        Map<String, PredefinedTag> byName = new LinkedHashMap<>();
        availableTags.forEach(t -> byName.put(normalizeTagName(t.getName()), t));
        unmatchedTags.clear();
        LinkedHashSet<PredefinedTag> selected = new LinkedHashSet<>();
        for (String token : splitTags(stored)) {
            PredefinedTag match = byName.get(normalizeTagName(token));
            if (match != null) {
                selected.add(match);
            } else {
                unmatchedTags.add(token);
            }
        }
        tagSelector.setValue(selected);
        updateTagHelper();
    }

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

    public void updateTagHelper() {
        String helper = t("position.tags.help");
        if (!unmatchedTags.isEmpty()) {
            helper += " " + t("position.tags.legacy", String.join(", ", unmatchedTags));
        }
        tagSelector.setHelperText(helper);
    }

    public String tagLabel(PredefinedTag tag) {
        return "[" + tagCategoryLabel(tag.getCategory()) + "] " + tag.getName();
    }

    private String tagCategoryLabel(String category) {
        String n = category == null ? "general" : category.trim().toLowerCase(Locale.ROOT);
        return switch (n) {
            case "order" -> t("settings.tags.cat.order");
            case "position" -> t("settings.tags.cat.position");
            default -> t("settings.tags.cat.general");
        };
    }

    private String normalizeTagName(String v) {
        return v == null ? "" : v.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> splitTags(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String tok : raw.split(",")) {
            String trimmed = tok.trim();
            if (!trimmed.isBlank()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private String t(String key, Object... params) {
        return translationSource.getTranslation(key, params);
    }
}
