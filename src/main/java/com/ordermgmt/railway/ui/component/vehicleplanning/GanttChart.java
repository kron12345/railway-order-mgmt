package com.ordermgmt.railway.ui.component.vehicleplanning;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.vaadin.tltv.gantt.Gantt;
import org.vaadin.tltv.gantt.model.Resolution;
import org.vaadin.tltv.gantt.model.Step;
import org.vaadin.tltv.gantt.model.SubStep;

import com.vaadin.flow.component.html.Div;

import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpRotationEntry;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpVehicle;

/**
 * Gantt chart component for vehicle rotation planning using the tltv Gantt Flow add-on. Each row
 * (Step) represents either a shelf (unassigned trains) or a duty (vehicle). Each block (SubStep)
 * represents a train placed in that row. Supports drag-and-drop between rows.
 */
public class GanttChart extends Div {

    /** UID prefix for shelf rows to distinguish them from duty rows. */
    public static final String SHELF_PREFIX = "shelf-";

    /** UID prefix for duty rows (uses the VpVehicle UUID). */
    public static final String DUTY_PREFIX = "duty-";

    /** UID prefix for train sub-steps (uses the PmReferenceTrain UUID). */
    public static final String TRAIN_PREFIX = "train-";

    /** UID prefix for entry sub-steps (uses the VpRotationEntry UUID). */
    public static final String ENTRY_PREFIX = "entry-";

    private static final String COLOR_SHELF_TRAIN = "#FFB800";
    private static final String COLOR_DUTY_TRAIN = "#009688";
    private static final String COLOR_SHELF_ROW = "#FFFDE7";
    private static final String COLOR_DUTY_ROW = "#E0F2F1";

    private final Gantt gantt;
    private MoveHandler moveHandler;

    /** Callback for when a substep is moved to a new row. */
    @FunctionalInterface
    public interface MoveHandler {
        /**
         * Called when a train block is moved.
         *
         * @param subStepUid the moved sub-step UID (train- or entry- prefix)
         * @param newOwnerUid the target row UID (shelf- or duty- prefix)
         */
        void onMove(String subStepUid, String newOwnerUid);
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

        gantt.addStepMoveListener(
                event -> {
                    if (moveHandler == null) {
                        return;
                    }
                    var movedStep = event.getAnyStep();
                    String newOwnerUid = event.getNewUid();
                    if (movedStep != null && newOwnerUid != null) {
                        moveHandler.onMove(movedStep.getUid(), newOwnerUid);
                    }
                });

        add(gantt);
        setSizeFull();
    }

    public void setMoveHandler(MoveHandler handler) {
        this.moveHandler = handler;
    }

    /**
     * Sets the visible time range for the Gantt chart.
     *
     * @param start start of the range
     * @param end end of the range
     */
    public void setTimeRange(LocalDateTime start, LocalDateTime end) {
        gantt.setStartDateTime(start);
        gantt.setEndDateTime(end);
    }

    /**
     * Loads and renders all data: shelf rows for unassigned trains and duty rows for vehicles.
     *
     * @param unassignedTrains trains not yet assigned to any vehicle
     * @param vehicles the vehicles (duties) with their rotation entries
     * @param baseDate the reference date for time calculations (determines the day shown)
     */
    public void loadData(
            List<PmReferenceTrain> unassignedTrains,
            List<VpVehicle> vehicles,
            LocalDateTime baseDate) {
        clearAll();

        // Set time range to cover a full day
        setTimeRange(baseDate.with(LocalTime.MIN), baseDate.with(LocalTime.MAX));

        // Build shelf rows from unassigned trains
        List<List<PmReferenceTrain>> shelves = calculateShelves(unassignedTrains, baseDate);
        for (int i = 0; i < shelves.size(); i++) {
            addShelfRow(i, shelves.get(i), baseDate);
        }

        // Build duty rows from vehicles
        for (VpVehicle vehicle : vehicles) {
            addDutyRow(vehicle, baseDate);
        }
    }

    /** Removes all steps from the Gantt chart. */
    public void clearAll() {
        List<Step> steps = new ArrayList<>(gantt.getStepsList());
        gantt.removeSteps(steps);
    }

    /** Returns the underlying Gantt component (for embedding in layouts). */
    public Gantt getGantt() {
        return gantt;
    }

    private void addShelfRow(int index, List<PmReferenceTrain> trains, LocalDateTime baseDate) {
        Step shelfRow = new Step();
        shelfRow.setUid(SHELF_PREFIX + index);
        shelfRow.setCaption("Shelf " + (index + 1));
        shelfRow.setBackgroundColor(COLOR_SHELF_ROW);
        shelfRow.setStartDate(baseDate.with(LocalTime.MIN));
        shelfRow.setEndDate(baseDate.with(LocalTime.MAX));
        shelfRow.setMovable(false);
        shelfRow.setResizable(false);
        gantt.addStep(shelfRow);

        for (PmReferenceTrain train : trains) {
            SubStep sub = createTrainSubStep(train, shelfRow, baseDate, COLOR_SHELF_TRAIN);
            gantt.addSubStep(sub);
        }
    }

    private void addDutyRow(VpVehicle vehicle, LocalDateTime baseDate) {
        Step dutyRow = new Step();
        dutyRow.setUid(DUTY_PREFIX + vehicle.getId());
        dutyRow.setCaption(vehicle.getLabel());
        dutyRow.setBackgroundColor(COLOR_DUTY_ROW);
        dutyRow.setStartDate(baseDate.with(LocalTime.MIN));
        dutyRow.setEndDate(baseDate.with(LocalTime.MAX));
        dutyRow.setMovable(false);
        dutyRow.setResizable(false);
        gantt.addStep(dutyRow);

        List<VpRotationEntry> entries =
                vehicle.getEntries().stream()
                        .sorted(Comparator.comparingInt(VpRotationEntry::getSequenceInDay))
                        .toList();

        for (VpRotationEntry entry : entries) {
            SubStep sub = createEntrySubStep(entry, dutyRow, baseDate);
            gantt.addSubStep(sub);
        }
    }

    private SubStep createTrainSubStep(
            PmReferenceTrain train, Step owner, LocalDateTime baseDate, String color) {
        SubStep sub = new SubStep(owner);
        sub.setUid(TRAIN_PREFIX + train.getId());
        sub.setCaption(buildTrainLabel(train));
        sub.setBackgroundColor(color);
        sub.setMovable(true);
        sub.setResizable(false);

        TrainTimeWindow tw = resolveTrainTimes(train, baseDate);
        sub.setStartDate(tw.start());
        sub.setEndDate(tw.end());

        return sub;
    }

    private SubStep createEntrySubStep(VpRotationEntry entry, Step owner, LocalDateTime baseDate) {
        PmReferenceTrain train = entry.getReferenceTrain();
        SubStep sub = new SubStep(owner);
        sub.setUid(ENTRY_PREFIX + entry.getId());
        sub.setCaption(buildTrainLabel(train));
        sub.setBackgroundColor(COLOR_DUTY_TRAIN);
        sub.setMovable(true);
        sub.setResizable(false);

        TrainTimeWindow tw = resolveTrainTimes(train, baseDate);
        sub.setStartDate(tw.start());
        sub.setEndDate(tw.end());

        return sub;
    }

    private String buildTrainLabel(PmReferenceTrain train) {
        String otn = train.getOperationalTrainNumber();
        return otn != null ? otn : train.getTridCore();
    }

    /**
     * Resolves departure/arrival times for a train into a time window on the given base date. Falls
     * back to a 1-hour placeholder if no journey data is available.
     */
    private TrainTimeWindow resolveTrainTimes(PmReferenceTrain train, LocalDateTime baseDate) {
        // Try to get times from the latest train version's journey locations.
        // Both trainVersions and journeyLocations are lazy-loaded; if the Hibernate
        // session is closed (typical in Vaadin UI threads), we fall through to the
        // fallback placeholder.
        try {
            var versions = train.getTrainVersions();
            if (versions != null && !versions.isEmpty()) {
                var latest =
                        versions.stream()
                                .max(
                                        Comparator.comparingInt(
                                                com.ordermgmt.railway.domain.pathmanager.model
                                                                .PmTrainVersion
                                                        ::getVersionNumber))
                                .orElse(null);
                if (latest != null) {
                    String depTime = null;
                    String arrTime = null;
                    var jlocs = latest.getJourneyLocations();
                    if (jlocs != null && !jlocs.isEmpty()) {
                        var sorted =
                                jlocs.stream()
                                        .sorted(
                                                Comparator.comparingInt(
                                                        com.ordermgmt.railway.domain.pathmanager
                                                                        .model.PmJourneyLocation
                                                                ::getSequence))
                                        .toList();
                        depTime = sorted.getFirst().getDepartureTime();
                        arrTime = sorted.getLast().getArrivalTime();
                    }

                    if (depTime != null && arrTime != null) {
                        LocalDateTime start = parseTimeOnDate(depTime, baseDate);
                        LocalDateTime end = parseTimeOnDate(arrTime, baseDate);
                        if (end.isAfter(start)) {
                            return new TrainTimeWindow(start, end);
                        }
                    }
                }
            }
        } catch (org.hibernate.LazyInitializationException ignored) {
            // Train versions or journey locations not in session; use fallback
        }

        // Fallback: 1-hour placeholder block at 06:00
        LocalDateTime fallbackStart = baseDate.with(LocalTime.of(6, 0));
        return new TrainTimeWindow(fallbackStart, fallbackStart.plusHours(1));
    }

    /** Parses a time string (HH:mm or HH:mm:ss) and places it on the given base date. */
    private LocalDateTime parseTimeOnDate(String time, LocalDateTime baseDate) {
        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            // Handle times > 24h (next day) by clamping
            if (hour >= 24) {
                hour = 23;
                minute = 59;
            }
            return baseDate.with(LocalTime.of(hour, minute));
        } catch (Exception e) {
            return baseDate.with(LocalTime.of(6, 0));
        }
    }

    /**
     * Distributes trains into shelf rows so that no two trains in the same shelf overlap in time.
     * Uses a greedy first-fit algorithm.
     */
    private List<List<PmReferenceTrain>> calculateShelves(
            List<PmReferenceTrain> trains, LocalDateTime baseDate) {
        List<List<PmReferenceTrain>> shelves = new ArrayList<>();
        List<List<TrainTimeWindow>> shelfWindows = new ArrayList<>();

        List<PmReferenceTrain> sorted =
                trains.stream()
                        .sorted(Comparator.comparing(t -> resolveTrainTimes(t, baseDate).start()))
                        .toList();

        for (PmReferenceTrain train : sorted) {
            TrainTimeWindow tw = resolveTrainTimes(train, baseDate);
            boolean placed = false;
            for (int i = 0; i < shelves.size(); i++) {
                if (!overlapsAny(tw, shelfWindows.get(i))) {
                    shelves.get(i).add(train);
                    shelfWindows.get(i).add(tw);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                List<PmReferenceTrain> newShelf = new ArrayList<>();
                newShelf.add(train);
                shelves.add(newShelf);
                List<TrainTimeWindow> newWindows = new ArrayList<>();
                newWindows.add(tw);
                shelfWindows.add(newWindows);
            }
        }

        // Always have at least one shelf row even if empty
        if (shelves.isEmpty()) {
            shelves.add(new ArrayList<>());
        }
        return shelves;
    }

    private boolean overlapsAny(TrainTimeWindow candidate, List<TrainTimeWindow> existing) {
        for (TrainTimeWindow tw : existing) {
            if (candidate.start().isBefore(tw.end()) && candidate.end().isAfter(tw.start())) {
                return true;
            }
        }
        return false;
    }

    private record TrainTimeWindow(LocalDateTime start, LocalDateTime end) {}
}
