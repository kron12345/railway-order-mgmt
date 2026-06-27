package com.ordermgmt.railway.ui.view.order;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.domain.order.model.FristRegel;
import com.ordermgmt.railway.domain.order.model.FristRegel.Action;
import com.ordermgmt.railway.domain.order.model.FristRegel.Anchor;
import com.ordermgmt.railway.domain.order.model.FristRegel.MemberFilter;
import com.ordermgmt.railway.domain.order.model.FristRegel.Trigger;
import com.ordermgmt.railway.domain.order.repository.FristRegelRepository;
import com.ordermgmt.railway.domain.order.service.AutoBusinessService;
import com.ordermgmt.railway.ui.layout.MainLayout;

/**
 * Admin CRUD for deadline rules (Frist-Regeln). Each saved rule is materialized as an automatic
 * business by {@link AutoBusinessService}, so rules are configured here in the GUI rather than via
 * SQL seeds. ADMIN-only.
 */
@Route(value = "fristregeln", layout = MainLayout.class)
@PageTitle("Frist-Regeln")
@RolesAllowed("ADMIN")
public class FristRegelAdminView extends VerticalLayout {

    private final FristRegelRepository regelRepository;
    private final AutoBusinessService autoBusinessService;
    private final Grid<FristRegel> grid = new Grid<>(FristRegel.class, false);

    public FristRegelAdminView(
            FristRegelRepository regelRepository, AutoBusinessService autoBusinessService) {
        this.regelRepository = regelRepository;
        this.autoBusinessService = autoBusinessService;
        setSizeFull();
        setPadding(true);
        add(buildHeader(), grid);
        configureGrid();
        refresh();
    }

    private HorizontalLayout buildHeader() {
        H2 title = new H2(getTranslation("fristRegel.title"));
        title.getStyle().set("margin", "0");
        Button add = new Button(getTranslation("fristRegel.new"), VaadinIcon.PLUS.create());
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        add.addClickListener(e -> openDialog(null));
        HorizontalLayout header = new HorizontalLayout(title, add);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return header;
    }

    private void configureGrid() {
        grid.addColumn(FristRegel::getName).setHeader(getTranslation("fristRegel.field.name"));
        grid.addColumn(r -> getTranslation("frist.memberFilter." + r.getMemberFilter().name()))
                .setHeader(getTranslation("fristRegel.field.memberFilter"));
        grid.addColumn(r -> getTranslation("frist.anchor." + r.getAnchor().name()))
                .setHeader(getTranslation("fristRegel.field.anchor"));
        grid.addColumn(r -> getTranslation("frist.trigger." + r.getTriggerType().name()))
                .setHeader(getTranslation("fristRegel.field.triggerType"));
        grid.addColumn(r -> getTranslation("frist.action." + r.getAction().name()))
                .setHeader(getTranslation("fristRegel.field.action"));
        grid.addColumn(r -> r.isEnabled() ? "✓" : "—")
                .setHeader(getTranslation("fristRegel.field.enabled"));
        grid.addComponentColumn(this::buildRowActions).setHeader("");
        grid.setSizeFull();
    }

    private HorizontalLayout buildRowActions(FristRegel rule) {
        Button edit = new Button(VaadinIcon.EDIT.create());
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        edit.getElement().setAttribute("aria-label", getTranslation("fristRegel.edit"));
        edit.addClickListener(e -> openDialog(rule));

        Button delete = new Button(VaadinIcon.TRASH.create());
        delete.addThemeVariants(
                ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        delete.getElement().setAttribute("aria-label", getTranslation("fristRegel.delete"));
        delete.addClickListener(e -> deleteRule(rule));
        return new HorizontalLayout(edit, delete);
    }

    private void deleteRule(FristRegel rule) {
        autoBusinessService.removeFor(rule.getId());
        regelRepository.delete(rule);
        refresh();
        Notification.show(getTranslation("fristRegel.deleted"));
    }

    private void refresh() {
        List<FristRegel> rules = regelRepository.findAll();
        grid.setItems(rules);
    }

    private void openDialog(FristRegel existing) {
        boolean isNew = existing == null;
        FristRegel rule = isNew ? new FristRegel() : existing;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation(isNew ? "fristRegel.new" : "fristRegel.edit"));
        dialog.setWidth("520px");

        TextField name = new TextField(getTranslation("fristRegel.field.name"));
        name.setValue(rule.getName() == null ? "" : rule.getName());
        name.setRequiredIndicatorVisible(true);

        Select<MemberFilter> memberFilter = enumSelect("memberFilter", MemberFilter.values());
        memberFilter.setLabel(getTranslation("fristRegel.field.memberFilter"));
        memberFilter.setValue(rule.getMemberFilter());

        Select<Anchor> anchor = enumSelect("anchor", Anchor.values());
        anchor.setLabel(getTranslation("fristRegel.field.anchor"));
        anchor.setValue(rule.getAnchor());

        DatePicker absoluteDate = new DatePicker(getTranslation("fristRegel.field.absoluteDate"));
        absoluteDate.setValue(rule.getAbsoluteDate());

        IntegerField offsetDays = new IntegerField(getTranslation("fristRegel.field.offsetDays"));
        offsetDays.setStepButtonsVisible(true);
        offsetDays.setValue(rule.getOffsetDays() != null ? rule.getOffsetDays() : 0);

        IntegerField warn = new IntegerField(getTranslation("fristRegel.field.warnThresholdDays"));
        warn.setMin(0);
        warn.setStepButtonsVisible(true);
        warn.setValue(rule.getWarnThresholdDays() != null ? rule.getWarnThresholdDays() : 7);

        Select<Trigger> triggerType = enumSelect("trigger", Trigger.values());
        triggerType.setLabel(getTranslation("fristRegel.field.triggerType"));
        triggerType.setValue(rule.getTriggerType());

        TextField triggerStatus = new TextField(getTranslation("fristRegel.field.triggerStatus"));
        triggerStatus.setValue(rule.getTriggerStatus() == null ? "" : rule.getTriggerStatus());

        Select<Action> action = enumSelect("action", Action.values());
        action.setLabel(getTranslation("fristRegel.field.action"));
        action.setValue(rule.getAction());

        Checkbox enabled = new Checkbox(getTranslation("fristRegel.field.enabled"));
        enabled.setValue(rule.isEnabled());

        // Anker- bzw. Trigger-abhängige Felder nur einblenden, wenn relevant.
        Runnable toggleConditional =
                () -> {
                    absoluteDate.setVisible(anchor.getValue() == Anchor.ABSOLUT);
                    offsetDays.setVisible(anchor.getValue() != Anchor.ABSOLUT);
                    triggerStatus.setVisible(triggerType.getValue() == Trigger.STATUS);
                };
        anchor.addValueChangeListener(e -> toggleConditional.run());
        triggerType.addValueChangeListener(e -> toggleConditional.run());
        toggleConditional.run();

        FormLayout form =
                new FormLayout(
                        name,
                        memberFilter,
                        anchor,
                        absoluteDate,
                        offsetDays,
                        warn,
                        triggerType,
                        triggerStatus,
                        action,
                        enabled);
        dialog.add(form);

        Button cancel = new Button(getTranslation("common.cancel"), e -> dialog.close());
        Button save = new Button(getTranslation("common.save"));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(
                e -> {
                    if (name.getValue() == null || name.getValue().isBlank()) {
                        name.setInvalid(true);
                        name.setErrorMessage(getTranslation("fristRegel.field.name.required"));
                        return;
                    }
                    rule.setName(name.getValue().trim());
                    rule.setMemberFilter(memberFilter.getValue());
                    rule.setAnchor(anchor.getValue());
                    rule.setAbsoluteDate(
                            anchor.getValue() == Anchor.ABSOLUT ? absoluteDate.getValue() : null);
                    rule.setOffsetDays(offsetDays.getValue() != null ? offsetDays.getValue() : 0);
                    rule.setWarnThresholdDays(warn.getValue() != null ? warn.getValue() : 0);
                    rule.setTriggerType(triggerType.getValue());
                    rule.setTriggerStatus(
                            triggerType.getValue() == Trigger.STATUS
                                    ? blankToNull(triggerStatus.getValue())
                                    : null);
                    rule.setAction(action.getValue());
                    rule.setEnabled(enabled.getValue());
                    regelRepository.save(rule);
                    autoBusinessService.syncAll();
                    dialog.close();
                    refresh();
                    Notification.show(getTranslation("fristRegel.saved"));
                });
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private <E extends Enum<E>> Select<E> enumSelect(String i18nGroup, E[] values) {
        Select<E> select = new Select<>();
        select.setItems(values);
        select.setItemLabelGenerator(v -> getTranslation("frist." + i18nGroup + "." + v.name()));
        return select;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
