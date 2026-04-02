package com.ordermgmt.railway.domain.pathmanager.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.pathmanager.model.PathAction;
import com.ordermgmt.railway.domain.pathmanager.model.PathProcessState;
import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
import com.ordermgmt.railway.domain.pathmanager.model.PmProcessStep;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;
import com.ordermgmt.railway.domain.pathmanager.model.ProcessStepResult;
import com.ordermgmt.railway.domain.pathmanager.model.VersionType;
import com.ordermgmt.railway.domain.pathmanager.repository.PmProcessStepRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmReferenceTrainRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTrainVersionRepository;

import lombok.RequiredArgsConstructor;

/**
 * State machine for the TTT path request lifecycle.
 *
 * <p>Validates allowed transitions and creates audit records (process steps). When a transition
 * produces new train data (offers, modifications), a new PmTrainVersion is created by copying
 * journey locations from the latest version.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PathProcessEngine {

    private final PmReferenceTrainRepository referenceTrainRepository;
    private final PmTrainVersionRepository trainVersionRepository;
    private final PmProcessStepRepository processStepRepository;

    /** Static transition table: state -> allowed actions. */
    private static final Map<PathProcessState, Set<PathAction>> ALLOWED_TRANSITIONS;

    /** Actions that produce a new train version by copying the latest version. */
    private static final Set<PathAction> VERSION_CREATING_ACTIONS =
            EnumSet.of(
                    PathAction.IM_DRAFT_OFFER,
                    PathAction.IM_ALTERATION_OFFER,
                    PathAction.IM_FINAL_OFFER);

    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(PathProcessState.class);

        ALLOWED_TRANSITIONS.put(PathProcessState.NEW, EnumSet.of(PathAction.SEND_REQUEST));

        ALLOWED_TRANSITIONS.put(
                PathProcessState.CREATED,
                EnumSet.of(PathAction.MODIFY_REQUEST, PathAction.WITHDRAW, PathAction.IM_RECEIPT));

        ALLOWED_TRANSITIONS.put(
                PathProcessState.MODIFIED,
                EnumSet.of(
                        PathAction.WITHDRAW,
                        PathAction.IM_RECEIPT,
                        PathAction.IM_DRAFT_OFFER,
                        PathAction.IM_NO_ALTERNATIVE));

        ALLOWED_TRANSITIONS.put(
                PathProcessState.RECEIPT_CONFIRMED,
                EnumSet.of(
                        PathAction.MODIFY_REQUEST,
                        PathAction.WITHDRAW,
                        PathAction.IM_DRAFT_OFFER,
                        PathAction.IM_NO_ALTERNATIVE,
                        PathAction.IM_ERROR));

        ALLOWED_TRANSITIONS.put(
                PathProcessState.DRAFT_OFFERED,
                EnumSet.of(
                        PathAction.REJECT_WITH_REVISION,
                        PathAction.REJECT_WITHOUT_REVISION,
                        PathAction.IM_FINAL_OFFER));

        ALLOWED_TRANSITIONS.put(
                PathProcessState.REVISION_REQUESTED,
                EnumSet.of(PathAction.IM_DRAFT_OFFER, PathAction.IM_NO_ALTERNATIVE));

        ALLOWED_TRANSITIONS.put(
                PathProcessState.FINAL_OFFERED,
                EnumSet.of(PathAction.ACCEPT_OFFER, PathAction.REJECT_WITHOUT_REVISION));

        ALLOWED_TRANSITIONS.put(
                PathProcessState.BOOKED,
                EnumSet.of(
                        PathAction.REQUEST_MODIFICATION,
                        PathAction.CANCEL_PATH,
                        PathAction.IM_ANNOUNCE_ALTERATION));

        ALLOWED_TRANSITIONS.put(
                PathProcessState.MODIFICATION_REQUESTED,
                EnumSet.of(
                        PathAction.IM_RECEIPT,
                        PathAction.IM_FINAL_OFFER,
                        PathAction.IM_NO_ALTERNATIVE));

        ALLOWED_TRANSITIONS.put(
                PathProcessState.ALTERATION_ANNOUNCED,
                EnumSet.of(PathAction.IM_ALTERATION_OFFER, PathAction.IM_NO_ALTERNATIVE));

        ALLOWED_TRANSITIONS.put(
                PathProcessState.ALTERATION_OFFERED,
                EnumSet.of(PathAction.ACCEPT_ALTERATION, PathAction.REJECT_ALTERATION));
    }

    /** Returns the actions available for the current state of the given reference train. */
    @Transactional(readOnly = true)
    public Set<PathAction> getAvailableActions(UUID referenceTrainId) {
        PmReferenceTrain train = loadTrain(referenceTrainId);
        Set<PathAction> actions = ALLOWED_TRANSITIONS.get(train.getProcessState());
        return actions != null ? EnumSet.copyOf(actions) : EnumSet.noneOf(PathAction.class);
    }

    /**
     * Executes a state transition on the given reference train.
     *
     * @param referenceTrainId the reference train to transition
     * @param action the action to execute
     * @param comment optional user comment
     * @return the result including the process step and optional new version
     */
    public ProcessStepResult executeTransition(
            UUID referenceTrainId, PathAction action, String comment) {

        PmReferenceTrain train = loadTrain(referenceTrainId);
        PathProcessState currentState = train.getProcessState();
        validateTransition(currentState, action);

        PathProcessState newState = resolveTargetState(currentState, action);

        PmProcessStep step = new PmProcessStep();
        step.setReferenceTrain(train);
        step.setStepType(action.name());
        step.setFromState(currentState.name());
        step.setToState(newState.name());
        step.setComment(comment);

        train.setProcessState(newState);
        referenceTrainRepository.save(train);

        PmTrainVersion newVersion = null;
        if (VERSION_CREATING_ACTIONS.contains(action)) {
            newVersion = createVersionFromLatest(train, versionTypeForAction(action));
            step.setPath(newVersion.getPath());
        }

        step = processStepRepository.save(step);
        return new ProcessStepResult(step, newState, newVersion);
    }

    private PmReferenceTrain loadTrain(UUID referenceTrainId) {
        return referenceTrainRepository
                .findById(referenceTrainId)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Reference train not found: " + referenceTrainId));
    }

    private void validateTransition(PathProcessState currentState, PathAction action) {
        Set<PathAction> allowed = ALLOWED_TRANSITIONS.get(currentState);
        if (allowed == null || !allowed.contains(action)) {
            throw new IllegalStateException(
                    "Action "
                            + action
                            + " is not allowed in state "
                            + currentState
                            + ". Allowed: "
                            + (allowed != null ? allowed : "none"));
        }
    }

    /** Determines the target state based on current state and action. */
    private PathProcessState resolveTargetState(PathProcessState currentState, PathAction action) {
        return switch (action) {
            case SEND_REQUEST -> PathProcessState.CREATED;
            case MODIFY_REQUEST -> PathProcessState.MODIFIED;
            case WITHDRAW -> PathProcessState.WITHDRAWN;
            case IM_RECEIPT -> PathProcessState.RECEIPT_CONFIRMED;
            case IM_DRAFT_OFFER -> PathProcessState.DRAFT_OFFERED;
            case IM_NO_ALTERNATIVE -> PathProcessState.NO_ALTERNATIVE;
            case IM_ERROR -> PathProcessState.NO_ALTERNATIVE;
            case REJECT_WITH_REVISION -> PathProcessState.REVISION_REQUESTED;
            case REJECT_WITHOUT_REVISION -> PathProcessState.WITHDRAWN;
            case IM_FINAL_OFFER -> PathProcessState.FINAL_OFFERED;
            case ACCEPT_OFFER -> PathProcessState.BOOKED;
            case IM_BOOK -> PathProcessState.BOOKED;
            case REQUEST_MODIFICATION -> PathProcessState.MODIFICATION_REQUESTED;
            case CANCEL_PATH -> PathProcessState.CANCELED;
            case IM_ANNOUNCE_ALTERATION -> PathProcessState.ALTERATION_ANNOUNCED;
            case IM_ALTERATION_OFFER -> PathProcessState.ALTERATION_OFFERED;
            case ACCEPT_ALTERATION -> PathProcessState.BOOKED;
            case REJECT_ALTERATION -> PathProcessState.BOOKED;
        };
    }

    private VersionType versionTypeForAction(PathAction action) {
        return switch (action) {
            case IM_DRAFT_OFFER, IM_FINAL_OFFER -> VersionType.MODIFICATION;
            case IM_ALTERATION_OFFER -> VersionType.ALTERATION;
            default -> VersionType.INITIAL;
        };
    }

    /**
     * Creates a new train version by copying journey locations from the latest version. This is
     * used when the IM provides an offer with potentially modified train data.
     */
    private PmTrainVersion createVersionFromLatest(
            PmReferenceTrain train, VersionType versionType) {
        PmTrainVersion latest =
                trainVersionRepository
                        .findFirstByReferenceTrainIdOrderByVersionNumberDesc(train.getId())
                        .orElse(null);

        int nextNumber = latest != null ? latest.getVersionNumber() + 1 : 1;

        PmTrainVersion newVersion = new PmTrainVersion();
        newVersion.setReferenceTrain(train);
        newVersion.setVersionNumber(nextNumber);
        newVersion.setVersionType(versionType);
        newVersion.setLabel(versionType.name() + " v" + nextNumber);

        if (latest != null) {
            copyTrainHeaderFromVersion(latest, newVersion);
            newVersion = trainVersionRepository.save(newVersion);
            copyJourneyLocations(latest, newVersion);
        } else {
            copyTrainHeaderFromReferenceTrain(train, newVersion);
            newVersion = trainVersionRepository.save(newVersion);
        }

        return newVersion;
    }

    private void copyTrainHeaderFromVersion(PmTrainVersion source, PmTrainVersion target) {
        target.setOperationalTrainNumber(source.getOperationalTrainNumber());
        target.setTrainType(source.getTrainType());
        target.setTrafficTypeCode(source.getTrafficTypeCode());
        target.setTrainWeight(source.getTrainWeight());
        target.setTrainLength(source.getTrainLength());
        target.setTrainMaxSpeed(source.getTrainMaxSpeed());
        target.setCalendarStart(source.getCalendarStart());
        target.setCalendarEnd(source.getCalendarEnd());
        target.setCalendarBitmap(source.getCalendarBitmap());
        target.setOffsetToReference(source.getOffsetToReference());
    }

    private void copyTrainHeaderFromReferenceTrain(PmReferenceTrain source, PmTrainVersion target) {
        target.setOperationalTrainNumber(source.getOperationalTrainNumber());
        target.setTrainType(source.getTrainType());
        target.setTrafficTypeCode(source.getTrafficTypeCode());
        target.setTrainWeight(source.getTrainWeight());
        target.setTrainLength(source.getTrainLength());
        target.setTrainMaxSpeed(source.getTrainMaxSpeed());
        target.setCalendarStart(source.getCalendarStart());
        target.setCalendarEnd(source.getCalendarEnd());
        target.setCalendarBitmap(source.getCalendarBitmap());
    }

    private void copyJourneyLocations(PmTrainVersion source, PmTrainVersion target) {
        List<PmJourneyLocation> copies = new ArrayList<>();
        for (PmJourneyLocation original : source.getJourneyLocations()) {
            PmJourneyLocation copy = new PmJourneyLocation();
            copy.setTrainVersion(target);
            copy.setSequence(original.getSequence());
            copy.setCountryCodeIso(original.getCountryCodeIso());
            copy.setLocationPrimaryCode(original.getLocationPrimaryCode());
            copy.setPrimaryLocationName(original.getPrimaryLocationName());
            copy.setUopid(original.getUopid());
            copy.setJourneyLocationType(original.getJourneyLocationType());
            copy.setArrivalTime(original.getArrivalTime());
            copy.setArrivalOffset(original.getArrivalOffset());
            copy.setDepartureTime(original.getDepartureTime());
            copy.setDepartureOffset(original.getDepartureOffset());
            copy.setDwellTime(original.getDwellTime());
            copy.setArrivalQualifier(original.getArrivalQualifier());
            copy.setDepartureQualifier(original.getDepartureQualifier());
            copy.setSubsidiaryCode(original.getSubsidiaryCode());
            copy.setActivities(original.getActivities());
            copy.setNetworkSpecificParams(original.getNetworkSpecificParams());
            copies.add(copy);
        }
        target.getJourneyLocations().addAll(copies);
    }
}
