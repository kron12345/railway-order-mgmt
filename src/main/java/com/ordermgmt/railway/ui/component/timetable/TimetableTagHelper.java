package com.ordermgmt.railway.ui.component.timetable;

import java.util.LinkedHashSet;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.CheckboxGroup;

import com.ordermgmt.railway.domain.infrastructure.model.PredefinedTag;
import com.ordermgmt.railway.ui.util.TagSelectionHelper;

/**
 * Tag selection and matching logic for the timetable builder. Thin adapter that delegates to the
 * shared {@link TagSelectionHelper}.
 */
public class TimetableTagHelper {

    private final TagSelectionHelper delegate;

    public TimetableTagHelper(
            CheckboxGroup<PredefinedTag> tagSelector,
            List<PredefinedTag> availableTags,
            LinkedHashSet<String> unmatchedTags,
            Component translationSource) {
        this.delegate =
                new TagSelectionHelper(
                        tagSelector,
                        availableTags,
                        unmatchedTags,
                        (key, params) ->
                                translationSource.getTranslation(
                                        key, params != null ? params : new Object[0]));
    }

    public void readTags(String stored) {
        delegate.readTags(stored);
        updateTagHelper();
    }

    public String joinSelectedTags() {
        return delegate.joinSelectedTags();
    }

    public void updateTagHelper() {
        delegate.updateHelperText("position.tags.help", "position.tags.legacy");
    }

    public String tagLabel(PredefinedTag tag) {
        return delegate.tagLabel(tag);
    }
}
