package com.ordermgmt.railway.ui.view.order;

import java.util.function.BiFunction;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import com.ordermgmt.railway.domain.customer.model.Customer;
import com.ordermgmt.railway.domain.customer.repository.CustomerRepository;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;

public class OrderFormPanel extends Div {

    private final TextField orderNumber = new TextField();
    private final TextField name = new TextField();
    private final ComboBox<Customer> customerCombo = new ComboBox<>();
    private final ComboBox<ProcessStatus> processStatus = new ComboBox<>();
    private final TextField internalStatus = new TextField();
    private final TextArea comment = new TextArea();
    private final TextField tags = new TextField();
    private final DatePicker validFrom = new DatePicker();
    private final DatePicker validTo = new DatePicker();
    private final TextField timetableYear = new TextField();
    private final BiFunction<String, Object[], String> translator;

    public OrderFormPanel(
            Order order,
            CustomerRepository customerRepository,
            BiFunction<String, Object[], String> translator) {
        this.translator = translator;

        setWidthFull();
        getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "var(--lumo-space-m) var(--lumo-space-l)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("box-sizing", "border-box");

        H3 sectionTitle = new H3(t("order.edit"));
        sectionTitle
                .getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("margin", "0 0 var(--lumo-space-s) 0")
                .set("font-size", "var(--lumo-font-size-m)");

        orderNumber.setLabel(t("order.number"));
        orderNumber.setRequired(true);
        orderNumber.setMaxLength(50);
        orderNumber.setAllowedCharPattern("[A-Za-z0-9\\-_]");
        orderNumber.setWidthFull();

        name.setLabel(t("order.name"));
        name.setRequired(true);
        name.setMaxLength(255);
        name.setWidthFull();

        customerCombo.setLabel(t("order.customer"));
        customerCombo.setItems(customerRepository.findAll());
        customerCombo.setItemLabelGenerator(Customer::getName);
        customerCombo.setClearButtonVisible(true);
        customerCombo.setWidthFull();

        processStatus.setLabel(t("order.processStatus"));
        processStatus.setItems(ProcessStatus.values());
        processStatus.setItemLabelGenerator(s -> t("process." + s.name()));
        processStatus.setWidthFull();

        internalStatus.setLabel(t("order.internalStatus"));
        internalStatus.setWidthFull();

        comment.setLabel(t("order.comment"));
        comment.setMaxLength(2000);
        comment.setWidthFull();
        comment.setHeight("100px");

        tags.setLabel(t("order.tags"));
        tags.setWidthFull();

        validFrom.setLabel(t("order.validFrom"));
        validFrom.setWidthFull();

        validTo.setLabel(t("order.validTo"));
        validTo.setWidthFull();

        timetableYear.setLabel(t("order.timetableYear"));
        timetableYear.setWidthFull();

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("900px", 3));

        form.add(orderNumber, name, customerCombo);
        form.add(processStatus, internalStatus, timetableYear);
        form.add(validFrom, validTo, tags);
        form.setColspan(comment, 3);
        form.add(comment);

        readFrom(order);
        add(sectionTitle, form);
    }

    private void readFrom(Order order) {
        orderNumber.setValue(nvl(order.getOrderNumber()));
        name.setValue(nvl(order.getName()));
        customerCombo.setValue(order.getCustomer());
        processStatus.setValue(order.getProcessStatus());
        internalStatus.setValue(nvl(order.getInternalStatus()));
        comment.setValue(nvl(order.getComment()));
        tags.setValue(nvl(order.getTags()));
        validFrom.setValue(order.getValidFrom());
        validTo.setValue(order.getValidTo());
        timetableYear.setValue(nvl(order.getTimetableYearLabel()));
    }

    public void writeTo(Order order) {
        order.setOrderNumber(orderNumber.getValue().trim());
        order.setName(name.getValue().trim());
        order.setCustomer(customerCombo.getValue());
        order.setProcessStatus(processStatus.getValue());
        order.setInternalStatus(blankToNull(internalStatus.getValue()));
        order.setComment(blankToNull(comment.getValue()));
        order.setTags(blankToNull(tags.getValue()));
        order.setValidFrom(validFrom.getValue());
        order.setValidTo(validTo.getValue());
        order.setTimetableYearLabel(blankToNull(timetableYear.getValue()));
    }

    public boolean validate() {
        boolean valid = true;
        if (orderNumber.getValue().isBlank()) {
            orderNumber.setInvalid(true);
            valid = false;
        }
        if (name.getValue().isBlank()) {
            name.setInvalid(true);
            valid = false;
        }
        return valid;
    }

    private String t(String key) {
        return translator.apply(key, new Object[0]);
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private static String blankToNull(String s) {
        return s != null && !s.isBlank() ? s.trim() : null;
    }
}
