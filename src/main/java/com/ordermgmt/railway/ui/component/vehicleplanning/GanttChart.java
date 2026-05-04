package com.ordermgmt.railway.ui.component.vehicleplanning;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.vaadin.tltv.gantt.Gantt;
import org.vaadin.tltv.gantt.model.Resolution;
import org.vaadin.tltv.gantt.model.Step;
import org.vaadin.tltv.gantt.model.SubStep;

import com.vaadin.flow.component.html.Div;

import com.ordermgmt.railway.domain.vehicleplanning.model.TrainScheduleInfo;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpRotationEntry;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpVehicle;

/**
 * Gantt chart component showing a Mon–Fri week view for vehicle rotation planning. Shelf rows hold
 * unassigned trains; duty rows hold vehicles with their assigned trains.
 */
public class GanttChart extends Div {

    public static final String SHELF_PREFIX = "shelf-";
    public static final String DUTY_PREFIX = "duty-";
    public static final String TRAIN_PREFIX = "train-";
    public static final String ENTRY_PREFIX = "entry-";

    private static final String COLOR_SHELF_TRAIN = "#FFB800";
    private static final String COLOR_DUTY_TRAIN = "#009688";
    private static final String COLOR_SHELF_ROW = "#FFFDE7";
    private static final String COLOR_DUTY_ROW = "#E0F2F1";

    private final Gantt gantt;
    private MoveHandler moveHandler;
    private LocalDate weekStart; // Monday of the displayed week

    /** Callback when a substep is moved to a new row. */
    @FunctionalInterface
    public interface MoveHandler {
        void onMove(String subStepUid, String newOwnerUid, int dayOfWeek);
    }

    public GanttChart() {
        gantt = new Gantt();
        gantt.setResolution(Resolution.Hour);
        gantt.setWidthFull();
        gantt.setHeight("600px");
        gantt.setMovableSteps(false);
        gantt.setResizableSteps(false);
        gantt.setMovableStepsBetweenRows(true);
        gantt.setYearRowVisible(false);
        gantt.setMonthRowVisible(false);

        gantt.addStepMoveListener(event -> {
            if (moveHandler == null) {
                return;
            }
            var movedStep = event.getAnyStep();
            String newOwnerUid = event.getNewUid();
            if (movedStep != null && newOwnerUid != null) {
                int dayOfWeek = deriveDayOfWeek(movedStep.getStartDate());
                moveHandler.onMove(movedStep.getUid(), newOwnerUid, dayOfWeek);
            }
        });

        add(gantt);
        setSizeFull();
    }

    public void setMoveHandler(MoveHandler handler) {
        this.moveHandler = handler;
    }

    /**
     * Loads a Mon–Fri week view with shelf rows (unassigned trains) and duty rows (vehicles).
     *
     * @param unassigned schedule info for unassigned trains
     * @param vehicles vehicles with their rotation entries
     * @param scheduleMap train-ID → schedule lookup for assigned trains
     * @param monday the Monday of the week to display
     */
    public void loadData(
            List<TrainScheduleInfo> unassigned,
            List<VpVehicle> vehicles,
            Map<java.util.UUID, TrainScheduleInfo> scheduleMap,
            LocalDate monday) {
        clearAll();
        this.weekStart = monday;

        LocalDateTime rangeStart = monday.atStartOfDay();
        LocalDateTime rangeEnd = monday.plusDays(4).atTime(LocalTime.MAX); // Friday 23:59
        gantt.setStartDateTime(rangeStart);
        gantt.setEndDateTime(rangeEnd);

        // Shelf rows: unassigned trains distributed to avoid overlap (all shown on Monday)
        List<List<TrainScheduleInfo>> shelves = distributeShelves(unassigned);
        for (int i = 0; i < shelves.size(); i++) {
            addShelfRow(i, shelves.get(i));
        }

        // Duty rows: one per vehicle, entries positioned on their dayOfWeek
        for (VpVehicle vehicle : vehicles) {
            addDutyRow(vehicle, scheduleMap);
        }
    }

    public void clearAll() {
        List<Step> steps = new ArrayList<>(gantt.getStepsList());
        gantt.removeSteps(steps);
    }

    public Gantt getGantt() {
        return gantt;
    }

    // --- Shelf rows ---

    private void addShelfRow(int index, List<TrainScheduleInfo> trains) {
        Step row = createRow(SHELF_PREFIX + index, "Shelf " + (index + 1), COLOR_SHELF_ROW);
        gantt.addStep(row);

        for (TrainScheduleInfo info : trains) {
            SubStep sub = new SubStep(row);
            sub.setUid(TRAIN_PREFIX + info.trainId());
            sub.setCaption(info.label());
            sub.setBackgroundColor(COLOR_SHELF_TRAIN);
            sub.setMovable(true);
            sub.setResizable(false);
            // Show on Monday (day 1)
            sub.setStartDate(weekStart.atTime(info.departure()));
            sub.setEndDate(weekStart.atTime(info.arrival()));
            gantt.addSubStep(sub);
        }
    }

    // --- Duty rows ---

    private void addDutyRow(VpVehicle vehicle, Map<java.util.UUID, TrainScheduleInfo> scheduleMap) {
        Step row = createRow(DUTY_PREFIX + vehicle.getId(), vehicle.getLabel(), COLOR_DUTY_ROW);
        gantt.addStep(row);

        List<VpRotationEntry> entries = vehicle.getEntries().stream()
                .sorted(Comparator.comparingInt(VpRotationEntry::getDayOfWeek)
                        .thenComparingInt(VpRotationEntry::getSequenceInDay))
                .toList();

        for (VpRotationEntry entry : entries) {
            TrainScheduleInfo info = scheduleMap.get(entry.getReferenceTrain().getId());
            if (info == null) {
                continue;
            }

            int day = entry.getDayOfWeek(); // 1=Mon, 5=Fri
            if (day < 1 || day > 5) {
                continue;
            }
            LocalDate entryDate = weekStart.plusDays(day - 1);

            SubStep sub = new SubStep(row);
            sub.setUid(ENTRY_PREFIX + entry.getId());
            sub.setCaption(info.label());
            sub.setBackgroundColor(COLOR_DUTY_TRAIN);
            sub.setMovable(true);
            sub.setResizable(false);
            sub.setStartDate(entryDate.atTime(info.departure()));
            sub.setEndDate(entryDate.atTime(info.arrival()));
            gantt.addSubStep(sub);
        }
    }

    // --- Helpers ---

    private Step createRow(String uid, String caption, String bgColor) {
        Step row = new Step();
        row.setUid(uid);
        row.setCaption(caption);
        row.setBackgroundColor(bgColor);
        row.setStartDate(weekStart.atStartOfDay());
        row.setEndDate(weekStart.plusDays(4).atTime(LocalTime.MAX));
        row.setMovable(false);
        row.setResizable(false);
        return row;
    }

    private int deriveDayOfWeek(LocalDateTime dateTime) {
        if (dateTime == null || weekStart == null) {
            return 1;
        }
        int days = (int) ChronoUnit.DAYS.between(weekStart, dateTime.toLocalDate());
        int dow = days + 1;
        return Math.max(1, Math.min(5, dow));
    }

    /**
     * Distributes trains into shelf rows so that no two trains overlap in time. Uses a greedy
     * first-fit algorithm. Always returns at least one (possibly empty) shelf.
     */
    private List<List<TrainScheduleInfo>> distributeShelves(List<TrainScheduleInfo> trains) {
        List<List<TrainScheduleInfo>> shelves = new ArrayList<>();
        List<List<LocalTime>> shelfEnds = new ArrayList<>();

        List<TrainScheduleInfo> sorted = trains.stream()
                .sorted(Comparator.comparing(TrainScheduleInfo::departure))
                .toList();

        for (TrainScheduleInfo info : sorted) {
            boolean placed = false;
            for (int i = 0; i < shelves.size(); i++) {
                List<LocalTime> ends = shelfEnds.get(i);
                if (ends.isEmpty() || !info.departure().isBefore(ends.getLast())) {
                    shelves.get(i).add(info);
                    ends.add(info.arrival());
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                List<TrainScheduleInfo> newShelf = new ArrayList<>();
                newShelf.add(info);
                shelves.add(newShelf);
                List<LocalTime> newEnds = new ArrayList<>();
                newEnds.add(info.arrival());
                shelfEnds.add(newEnds);
            }
        }

        if (shelves.isEmpty()) {
            shelves.add(new ArrayList<>());
        }
        return shelves;
    }
}
