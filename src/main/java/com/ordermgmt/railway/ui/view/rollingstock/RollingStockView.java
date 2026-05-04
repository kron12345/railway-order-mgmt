package com.ordermgmt.railway.ui.view.rollingstock;

import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.domain.rollingstock.model.RollingStockItem;
import com.ordermgmt.railway.domain.rollingstock.model.VehicleCategory;
import com.ordermgmt.railway.domain.rollingstock.service.RollingStockService;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Card-based view for managing rolling stock master data (Fahrzeug-Stammdaten). */
@Route(value = "rollingstock", layout = MainLayout.class)
@PageTitle("Rolling Stock")
@jakarta.annotation.security.RolesAllowed({"ADMIN", "DISPATCHER"})
public class RollingStockView extends VerticalLayout {

    private final RollingStockService service;
    private final FlexLayout cardsContainer;

    public RollingStockView(RollingStockService service) {
        this.service = service;
        setPadding(true);
        setSpacing(true);

        H2 title = new H2(getTranslation("rs.title"));
        title.getStyle().set("margin", "0");

        Button addBtn = new Button(getTranslation("rs.add"), VaadinIcon.PLUS.create());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.addClickListener(e -> openDialog(new RollingStockItem()));

        HorizontalLayout header = new HorizontalLayout(title, addBtn);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        cardsContainer = new FlexLayout();
        cardsContainer.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        cardsContainer.getStyle().set("gap", "var(--lumo-space-m)");

        add(header, cardsContainer);
        refreshCards();
    }

    private void refreshCards() {
        cardsContainer.removeAll();
        List<RollingStockItem> items = service.findAll();
        for (RollingStockItem item : items) {
            cardsContainer.add(createCard(item));
        }
    }

    private Div createCard(RollingStockItem item) {
        Div card = new Div();
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-m)")
                .set("min-width", "280px")
                .set("max-width", "350px")
                .set("background", "var(--lumo-contrast-5pct)");

        // Header: designation + category badge
        Div headerDiv = new Div();
        Span name = new Span(item.getDesignation());
        name.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-l)");

        Span catBadge = new Span(getTranslation("rs.category." + item.getVehicleCategory().name()));
        catBadge.getStyle()
                .set("background", categoryColor(item.getVehicleCategory()))
                .set("color", "white")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("padding", "2px var(--lumo-space-xs)")
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("margin-left", "var(--lumo-space-s)");
        headerDiv.add(name, catBadge);

        // EVN
        if (item.getEvn() != null) {
            Div evnDiv = new Div();
            evnDiv.setText("EVN: " + item.getEvn());
            evnDiv.getStyle().set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-secondary-text-color)");
            card.add(evnDiv);
        }

        // Technical details
        Div details = new Div();
        details.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("margin-top", "var(--lumo-space-xs)");
        StringBuilder sb = new StringBuilder();
        if (item.getKeeperCode() != null) sb.append(item.getKeeperCode()).append(" | ");
        if (item.getLengthOverBuffers() != null)
            sb.append(String.format("%.1f m", item.getLengthOverBuffers() / 1000.0)).append(" | ");
        if (item.getWeightEmpty() != null)
            sb.append(String.format("%.0f t", item.getWeightEmpty() / 1000.0)).append(" | ");
        if (item.getMaxSpeed() != null) sb.append(item.getMaxSpeed()).append(" km/h");
        details.setText(sb.toString().replaceAll("\\| $", ""));

        // Capacity or payload
        Div capacityDiv = new Div();
        capacityDiv.getStyle().set("font-size", "var(--lumo-font-size-s)");
        if (item.getSeats1stClass() != null || item.getSeats2ndClass() != null) {
            String cap = "";
            if (item.getSeats1stClass() != null) cap += item.getSeats1stClass() + " (1.Kl) ";
            if (item.getSeats2ndClass() != null) cap += item.getSeats2ndClass() + " (2.Kl)";
            capacityDiv.setText("Kapazität: " + cap.trim());
        } else if (item.getMaxPayload() != null) {
            capacityDiv.setText("Nutzlast: " + String.format("%.1f t", item.getMaxPayload() / 1000.0));
        }
        if (item.getPowerOutput() != null) {
            Div powerDiv = new Div();
            powerDiv.setText("Leistung: " + item.getPowerOutput() + " kW");
            powerDiv.getStyle().set("font-size", "var(--lumo-font-size-s)");
            card.add(powerDiv);
        }

        // Edit button
        Button editBtn = new Button(VaadinIcon.EDIT.create());
        editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editBtn.addClickListener(e -> openDialog(item));
        editBtn.getStyle().set("float", "right").set("margin-top", "var(--lumo-space-xs)");

        card.addComponentAsFirst(headerDiv);
        card.add(details, capacityDiv, editBtn);
        return card;
    }

    private String categoryColor(VehicleCategory cat) {
        return switch (cat) {
            case LOCOMOTIVE -> "#1565C0";
            case EMU, DMU -> "#2E7D32";
            case COACH, CONTROL_CAR -> "#6A1B9A";
            case FREIGHT_WAGON -> "#E65100";
        };
    }

    // --- Edit Dialog ---

    private void openDialog(RollingStockItem item) {
        boolean isNew = item.getId() == null;
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? getTranslation("rs.add") : getTranslation("rs.edit"));
        dialog.setWidth("700px");

        TextField designation = new TextField(getTranslation("rs.designation"));
        designation.setValue(nvl(item.getDesignation()));
        designation.setRequired(true);

        ComboBox<VehicleCategory> category = new ComboBox<>(getTranslation("rs.category"));
        category.setItems(VehicleCategory.values());
        category.setItemLabelGenerator(c -> getTranslation("rs.category." + c.name()));
        category.setValue(item.getVehicleCategory() != null
                ? item.getVehicleCategory() : VehicleCategory.EMU);

        TextField evn = new TextField("EVN");
        evn.setValue(nvl(item.getEvn()));
        evn.setMaxLength(12);

        TextField keeper = new TextField(getTranslation("rs.keeper"));
        keeper.setValue(nvl(item.getKeeperCode()));

        TextField country = new TextField(getTranslation("rs.country"));
        country.setValue(nvl(item.getOwnerCountryCode()));

        TextField uicCode = new TextField("UIC-Kennzeichen");
        uicCode.setValue(nvl(item.getUicLetterCode()));

        IntegerField axles = new IntegerField(getTranslation("rs.axles"));
        axles.setValue(item.getNumberOfAxles());

        IntegerField length = new IntegerField(getTranslation("rs.length") + " (mm)");
        length.setValue(item.getLengthOverBuffers());

        IntegerField weight = new IntegerField(getTranslation("rs.weight") + " (kg)");
        weight.setValue(item.getWeightEmpty());

        IntegerField speed = new IntegerField("Vmax (km/h)");
        speed.setValue(item.getMaxSpeed());

        IntegerField power = new IntegerField(getTranslation("rs.power") + " (kW)");
        power.setValue(item.getPowerOutput());

        TextField traction = new TextField(getTranslation("rs.traction"));
        traction.setValue(nvl(item.getTractionSystem()));

        IntegerField seats1 = new IntegerField(getTranslation("rs.seats1"));
        seats1.setValue(item.getSeats1stClass());

        IntegerField seats2 = new IntegerField(getTranslation("rs.seats2"));
        seats2.setValue(item.getSeats2ndClass());

        IntegerField payload = new IntegerField(getTranslation("rs.payload") + " (kg)");
        payload.setValue(item.getMaxPayload());

        TextField coupling = new TextField(getTranslation("rs.coupling"));
        coupling.setValue(nvl(item.getCouplingType()));

        TextField brake = new TextField(getTranslation("rs.brake"));
        brake.setValue(nvl(item.getBrakeType()));

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2),
                new FormLayout.ResponsiveStep("600px", 3));
        form.add(designation, category, evn, keeper, country, uicCode,
                axles, length, weight, speed, power, traction,
                seats1, seats2, payload, coupling, brake);
        dialog.add(form);

        Button saveBtn = new Button(getTranslation("common.save"));
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClickListener(e -> {
            if (designation.getValue() == null || designation.getValue().isBlank()) {
                designation.setInvalid(true);
                return;
            }
            item.setDesignation(designation.getValue());
            item.setVehicleCategory(category.getValue());
            item.setEvn(blankToNull(evn.getValue()));
            item.setKeeperCode(blankToNull(keeper.getValue()));
            item.setOwnerCountryCode(blankToNull(country.getValue()));
            item.setUicLetterCode(blankToNull(uicCode.getValue()));
            item.setNumberOfAxles(axles.getValue());
            item.setLengthOverBuffers(length.getValue());
            item.setWeightEmpty(weight.getValue());
            item.setMaxSpeed(speed.getValue());
            item.setPowerOutput(power.getValue());
            item.setTractionSystem(blankToNull(traction.getValue()));
            item.setSeats1stClass(seats1.getValue());
            item.setSeats2ndClass(seats2.getValue());
            item.setMaxPayload(payload.getValue());
            item.setCouplingType(blankToNull(coupling.getValue()));
            item.setBrakeType(blankToNull(brake.getValue()));
            service.save(item);
            dialog.close();
            refreshCards();
            Notification.show(getTranslation("rs.saved"), 2000,
                    Notification.Position.BOTTOM_START);
        });
        Button cancelBtn = new Button(getTranslation("common.cancel"), e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private String nvl(String s) { return s != null ? s : ""; }
    private String blankToNull(String s) { return s != null && !s.isBlank() ? s : null; }
}
