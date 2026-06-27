package com.ordermgmt.railway.ui.util;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

import com.vaadin.flow.component.checkbox.CheckboxGroup;

import com.ordermgmt.railway.domain.infrastructure.model.PredefinedTag;

/** Reusable tag selection logic for CheckboxGroup-based tag pickers. */
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

    public void readTags(String stored) {
        Map<String, PredefinedTag> tagsByName = new LinkedHashMap<>();
        availableTags.forEach(tag -> tagsByName.put(normalizeTagName(tag.getName()), tag));
        unmatchedTags.clear();

        LinkedHashSet<PredefinedTag> selected = new LinkedHashSet<>();
        for (String token : StringUtils.splitTags(stored)) {
            PredefinedTag match = tagsByName.get(normalizeTagName(token));
            if (match != null) {
                selected.add(match);
            } else {
                unmatchedTags.add(token);
            }
        }
        tagSelector.setValue(selected);
    }

    public String joinSelectedTags() {
        LinkedHashSet<String> selectedTagNames = new LinkedHashSet<>();
        for (PredefinedTag tag : availableTags) {
            if (tagSelector.getValue().contains(tag)) {
                selectedTagNames.add(tag.getName());
            }
        }
        selectedTagNames.addAll(unmatchedTags);
        return selectedTagNames.isEmpty() ? null : String.join(", ", selectedTagNames);
    }

    public void updateHelperText(String baseHelperKey, String legacyKey) {
        String helper = translate(baseHelperKey);
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
        String normalizedCategory =
                category == null ? "general" : category.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedCategory) {
            case "order" -> translate("settings.tags.cat.order");
            case "position" -> translate("settings.tags.cat.position");
            default -> translate("settings.tags.cat.general");
        };
    }

    private String normalizeTagName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String translate(String key) {
        return translator.apply(key, new Object[0]);
    }
}
