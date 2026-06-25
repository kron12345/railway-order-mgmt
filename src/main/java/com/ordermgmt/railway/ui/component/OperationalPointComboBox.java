package com.ordermgmt.railway.ui.component;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.vaadin.flow.component.combobox.ComboBox;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;

/**
 * Lazy ComboBox for operational points: server-side, paged search by name/UOPID, so it never loads
 * all ~19,300 points into the browser. The dropdown fetches one page at a time as the user scrolls
 * or types; a value set programmatically (e.g. when editing an existing position) is kept even when
 * it is not on the current fetch page.
 */
public class OperationalPointComboBox extends ComboBox<OperationalPoint> {

    public OperationalPointComboBox(OperationalPointRepository repo) {
        setItemLabelGenerator(op -> op.getName() + " (" + op.getUopid() + ")");
        setItems(
                query -> {
                    String filter = query.getFilter().orElse("");
                    // Stable order (name, then unique UOPID as tie-breaker) so paged scrolling
                    // never duplicates or skips rows.
                    PageRequest pageable =
                            PageRequest.of(
                                    query.getPage(),
                                    query.getPageSize(),
                                    Sort.by("name").ascending().and(Sort.by("uopid").ascending()));
                    return repo
                            .findByNameContainingIgnoreCaseOrUopidContainingIgnoreCase(
                                    filter, filter, pageable)
                            .stream();
                });
    }
}
