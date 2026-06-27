package com.ordermgmt.railway.ui.component;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.vaadin.flow.component.combobox.ComboBox;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;

public class OperationalPointComboBox extends ComboBox<OperationalPoint> {

    private static final Sort SEARCH_SORT =
            Sort.by("name").ascending().and(Sort.by("uopid").ascending());

    public OperationalPointComboBox(OperationalPointRepository repository) {
        setItemLabelGenerator(OperationalPointComboBox::formatOperationalPoint);
        bindLazySearch(this, repository);
    }

    public static void bindLazySearch(
            ComboBox<OperationalPoint> comboBox, OperationalPointRepository repository) {
        comboBox.setItems(
                query -> {
                    String searchTerm = query.getFilter().orElse("");
                    PageRequest pageable =
                            PageRequest.of(query.getPage(), query.getPageSize(), SEARCH_SORT);
                    return repository.searchByNameOrUopid(searchTerm, pageable).stream();
                });
    }

    private static String formatOperationalPoint(OperationalPoint operationalPoint) {
        return operationalPoint.getName() + " (" + operationalPoint.getUopid() + ")";
    }
}
