package com.ordermgmt.railway.ui.component.order;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiFunction;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;

import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.order.model.CoverageType;
import com.ordermgmt.railway.domain.order.model.OperatingDays;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.model.ResourceCatalogItem;
import com.ordermgmt.railway.domain.order.model.ResourceType;
import com.ordermgmt.railway.domain.order.model.ValidityJsonCodec;
import com.ordermgmt.railway.domain.order.repository.ResourceCatalogItemRepository;
import com.ordermgmt.railway.domain.order.service.ResourceNeedService;
import com.ordermgmt.railway.ui.component.OperationalPointComboBox;
import com.ordermgmt.railway.ui.component.ValidityCalendar;

/** Dialog for adding a resource need to an order position. */
public class ResourceDialog extends Dialog {

    private static final String CAT_VEHICLE_TYPE = "VEHICLE_TYPE";
    private static final String CAT_PERSONNEL_QUAL = "PERSONNEL_QUAL";

    private final OrderPosition position;
    private final ResourceNeedService resourceNeedService;
    private final ResourceCatalogItemRepository catalogItemRepository;
    private final OperationalPointRepository opRepo;
    private final BiFunction<String, Object[], String> translator;

    public ResourceDialog(
            OrderPosition position,
            ResourceNeedService resourceNeedService,
            ResourceCatalogItemRepository catalogItemRepository,
            OperationalPointRepository opRepo,
            BiFunction<String, Object[], String> translator) {
        this.position = position;
        this.resourceNeedService = resourceNeedService;
        this.catalogItemRepository = catalogItemRepository;
        this.opRepo = opRepo;
        this.translator = translator;

        setHeaderTitle(tr("resource.add"));
        setWidth("500px");

        buildForm();
    }

    private void buildForm() {
        ResourceFormFields fields = createFormFields();
        RouteFields routeFields = createRouteFields();
        boolean timetablePosition = position.getType() == PositionType.FAHRPLAN;
        CalendarBounds calendarBounds = resolveCalendarBounds();
        ValidityCalendar calendar = createCalendar(calendarBounds);

        Div form =
                new Div(
                        fields.type(),
                        fields.coverage(),
                        fields.catalog(),
                        fields.quantity(),
                        fields.description());
        if (timetablePosition) {
            form.add(routeFields.from(), routeFields.to());
        }
        form.add(createCalendarWrapper(calendar));
        form.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-s)");
        add(form);

        Button cancel = new Button(tr("common.cancel"));
        cancel.addClickListener(event -> close());

        Button save = new Button(tr("common.save"));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        save.addClickListener(
                event ->
                        saveResource(
                                fields,
                                routeFields,
                                calendar,
                                calendarBounds.operatingDays(),
                                timetablePosition));

        getFooter().add(cancel, save);
    }

    private ResourceFormFields createFormFields() {
        Select<ResourceType> typeSelect = new Select<>();
        typeSelect.setLabel(tr("resource.type"));
        typeSelect.setItems(ResourceType.values());
        typeSelect.setItemLabelGenerator(t -> tr("resource.type." + t.name()));
        typeSelect.setValue(ResourceType.CAPACITY);
        typeSelect.setWidthFull();

        Select<CoverageType> coverageSelect = new Select<>();
        coverageSelect.setLabel(tr("resource.coverage"));
        coverageSelect.setItems(CoverageType.values());
        coverageSelect.setItemLabelGenerator(c -> tr("resource.coverage." + c.name()));
        coverageSelect.setValue(CoverageType.EXTERNAL);
        coverageSelect.setWidthFull();

        ComboBox<ResourceCatalogItem> catalogCombo = new ComboBox<>(tr("resource.catalogItem"));
        catalogCombo.setItemLabelGenerator(item -> item.getCode() + " — " + item.getName());
        catalogCombo.setWidthFull();
        catalogCombo.setClearButtonVisible(true);

        // Update catalog items when type changes
        typeSelect.addValueChangeListener(e -> updateCatalogItems(e.getValue(), catalogCombo));
        updateCatalogItems(typeSelect.getValue(), catalogCombo);

        IntegerField quantity = new IntegerField(tr("resource.quantity"));
        quantity.setMin(1);
        quantity.setValue(1);
        quantity.setStepButtonsVisible(true);
        quantity.setWidthFull();

        TextField description = new TextField(tr("resource.description"));
        description.setWidthFull();

        return new ResourceFormFields(
                typeSelect, coverageSelect, catalogCombo, quantity, description);
    }

    private RouteFields createRouteFields() {
        OperationalPointComboBox fromOp = new OperationalPointComboBox(opRepo);
        fromOp.setLabel(tr("position.from"));
        fromOp.setClearButtonVisible(true);
        fromOp.setWidthFull();
        OperationalPointComboBox toOp = new OperationalPointComboBox(opRepo);
        toOp.setLabel(tr("position.to"));
        toOp.setClearButtonVisible(true);
        toOp.setWidthFull();
        return new RouteFields(fromOp, toOp);
    }

    private CalendarBounds resolveCalendarBounds() {
        List<LocalDate> operatingDays =
                OperatingDays.of(position).stream().sorted().distinct().toList();
        LocalDate min;
        LocalDate max;
        if (!operatingDays.isEmpty()) {
            min = operatingDays.get(0);
            max = operatingDays.get(operatingDays.size() - 1);
        } else {
            min = position.getStart() != null ? position.getStart().toLocalDate() : LocalDate.now();
            max = position.getEnd() != null ? position.getEnd().toLocalDate() : min.plusYears(1);
        }
        if (max.isBefore(min)) {
            max = min;
        }
        return new CalendarBounds(min, max, operatingDays);
    }

    private ValidityCalendar createCalendar(CalendarBounds bounds) {
        ValidityCalendar calendar = new ValidityCalendar(bounds.min(), bounds.max());
        calendar.setCompact(true);
        if (!bounds.operatingDays().isEmpty()) {
            calendar.setAllowedDates(bounds.operatingDays());
            calendar.setSelectedDates(bounds.operatingDays());
        }
        return calendar;
    }

    private Div createCalendarWrapper(ValidityCalendar calendar) {
        Span calLabel = new Span(tr("resource.verkehrstage"));
        calLabel.getStyle().set("font-size", "13px").set("color", "var(--rom-text-secondary)");
        Div calWrap = new Div(calLabel, calendar);
        calWrap.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "4px");
        return calWrap;
    }

    private void saveResource(
            ResourceFormFields fields,
            RouteFields routeFields,
            ValidityCalendar calendar,
            List<LocalDate> operatingDays,
            boolean timetablePosition) {
        ResourceType type = fields.type().getValue();
        CoverageType coverage = fields.coverage().getValue();
        if (type == null || coverage == null) {
            return;
        }

        try {
            List<LocalDate> selectedDays = calendar.getSelectedDates();
            if (!operatingDays.isEmpty() && selectedDays.isEmpty()) {
                Notification.show(tr("verkehrstage.empty"), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            String validity =
                    selectedDays.isEmpty() ? null : ValidityJsonCodec.toJson(selectedDays);
            String fromLocation = selectedRouteName(timetablePosition, routeFields.from());
            String toLocation = selectedRouteName(timetablePosition, routeFields.to());
            ResourceCatalogItem catalogItem = fields.catalog().getValue();
            int quantity = fields.quantity().getValue() != null ? fields.quantity().getValue() : 1;

            var saved =
                    resourceNeedService.addResource(
                            position.getId(),
                            type,
                            catalogItem != null ? catalogItem.getId() : null,
                            quantity,
                            coverage,
                            fields.description().getValue());
            if (validity != null || fromLocation != null || toLocation != null) {
                resourceNeedService.updateVerkehrstageAndRoute(
                        saved.getId(), validity, fromLocation, toLocation);
            }

            Notification.show("OK", 2000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            close();
            fireEvent(new SaveEvent(this));
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private String selectedRouteName(boolean timetablePosition, OperationalPointComboBox field) {
        return timetablePosition && field.getValue() != null ? field.getValue().getName() : null;
    }

    private void updateCatalogItems(ResourceType type, ComboBox<ResourceCatalogItem> combo) {
        if (type == null) {
            combo.setItems(List.of());
            return;
        }
        String category =
                switch (type) {
                    case VEHICLE -> CAT_VEHICLE_TYPE;
                    case PERSONNEL -> CAT_PERSONNEL_QUAL;
                    case CAPACITY -> null;
                };
        if (category != null) {
            combo.setItems(catalogItemRepository.findByCategoryAndActiveTrue(category));
            combo.setVisible(true);
        } else {
            combo.setItems(List.of());
            combo.setVisible(false);
        }
    }

    /** Fired after a resource need is successfully saved. */
    public static class SaveEvent extends ComponentEvent<ResourceDialog> {
        public SaveEvent(ResourceDialog source) {
            super(source, false);
        }
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }

    private String tr(String key) {
        return translator.apply(key, new Object[0]);
    }

    private record ResourceFormFields(
            Select<ResourceType> type,
            Select<CoverageType> coverage,
            ComboBox<ResourceCatalogItem> catalog,
            IntegerField quantity,
            TextField description) {}

    private record RouteFields(OperationalPointComboBox from, OperationalPointComboBox to) {}

    private record CalendarBounds(LocalDate min, LocalDate max, List<LocalDate> operatingDays) {}
}
