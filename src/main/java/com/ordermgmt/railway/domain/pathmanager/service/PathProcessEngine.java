package com.ordermgmt.railway.domain.pathmanager.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.pathmanager.model.PathAction;
import com.ordermgmt.railway.domain.pathmanager.model.PathProcessState;
import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
import com.ordermgmt.railway.domain.pathmanager.model.PmProcessStep;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;
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

    private static final String ANONYMOUS_USER = "anonymous";

    private static final Map<PathProcessState, Set<PathAction>> ALLOWED_TRANSITIONS =
            createAllowedTransitions();

    private static final Set<PathAction> VERSION_CREATING_ACTIONS =
            EnumSet.of(
                    PathAction.IM_DRAFT_OFFER,
                    PathAction.IM_ALTERATION_OFFER,
                    PathAction.IM_FINAL_OFFER);

    private final PmReferenceTrainRepository referenceTrainRepository;
    private final PmTrainVersionRepository trainVersionRepository;
    private final PmProcessStepRepository processStepRepository;
    private final TtrPhaseResolver ttrPhaseResolver;

    private static Map<PathProcessState, Set<PathAction>> createAllowedTransitions() {
        Map<PathProcessState, Set<PathAction>> transitions = new EnumMap<>(PathProcessState.class);
        transitions.put(PathProcessState.NEW, EnumSet.of(PathAction.SEND_REQUEST));
        transitions.put(
                PathProcessState.CREATED,
                EnumSet.of(PathAction.MODIFY_REQUEST, PathAction.WITHDRAW, PathAction.IM_RECEIPT));
        transitions.put(
                PathProcessState.MODIFIED,
                EnumSet.of(
                        PathAction.WITHDRAW,
                        PathAction.IM_RECEIPT,
                        PathAction.IM_DRAFT_OFFER,
                        PathAction.IM_NO_ALTERNATIVE));
        transitions.put(
                PathProcessState.RECEIPT_CONFIRMED,
                EnumSet.of(
                        PathAction.MODIFY_REQUEST,
                        PathAction.WITHDRAW,
                        PathAction.IM_DRAFT_OFFER,
                        PathAction.IM_NO_ALTERNATIVE,
                        PathAction.IM_ERROR));
        transitions.put(
                PathProcessState.DRAFT_OFFERED,
                EnumSet.of(
                        PathAction.REJECT_WITH_REVISION,
                        PathAction.REJECT_WITHOUT_REVISION,
                        PathAction.IM_FINAL_OFFER));
        transitions.put(
                PathProcessState.REVISION_REQUESTED,
                EnumSet.of(PathAction.IM_DRAFT_OFFER, PathAction.IM_NO_ALTERNATIVE));
        transitions.put(
                PathProcessState.FINAL_OFFERED,
                EnumSet.of(PathAction.ACCEPT_OFFER, PathAction.REJECT_WITHOUT_REVISION));
        transitions.put(
                PathProcessState.BOOKED,
                EnumSet.of(
                        PathAction.REQUEST_MODIFICATION,
                        PathAction.CANCEL_PATH,
                        PathAction.IM_ANNOUNCE_ALTERATION));
        transitions.put(
                PathProcessState.MODIFICATION_REQUESTED,
                EnumSet.of(
                        PathAction.IM_RECEIPT,
                        PathAction.IM_FINAL_OFFER,
                        PathAction.IM_NO_ALTERNATIVE));
        transitions.put(
                PathProcessState.ALTERATION_ANNOUNCED,
                EnumSet.of(PathAction.IM_ALTERATION_OFFER, PathAction.IM_NO_ALTERNATIVE));
        transitions.put(
                PathProcessState.ALTERATION_OFFERED,
                EnumSet.of(PathAction.ACCEPT_ALTERATION, PathAction.REJECT_ALTERATION));
        return transitions;
    }

    /** Returns the actions available for the current state of the given reference train. */
    @Transactional(readOnly = true)
    public Set<PathAction> getAvailableActions(UUID referenceTrainId) {
        PmReferenceTrain train = loadTrain(referenceTrainId);
        Set<PathAction> actions = ALLOWED_TRANSITIONS.get(train.getProcessState());
        if (actions == null) {
            return EnumSet.noneOf(PathAction.class);
        }
        EnumSet<PathAction> filtered = EnumSet.copyOf(actions);

        if (shouldUseDirectFinalOffer(train)) {
            filtered.remove(PathAction.IM_DRAFT_OFFER);
            filtered.add(PathAction.IM_FINAL_OFFER);
        }

        return filtered;
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
        validateTransition(currentState, action, train);
        PathProcessState newState = resolveTargetState(action);
        PmProcessStep step = createProcessStep(train, action, currentState, newState, comment);

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

    private PmProcessStep createProcessStep(
            PmReferenceTrain train,
            PathAction action,
            PathProcessState fromState,
            PathProcessState toState,
            String comment) {
        PmProcessStep step = new PmProcessStep();
        step.setReferenceTrain(train);
        step.setStepType(action.name());
        step.setFromState(fromState.name());
        step.setToState(toState.name());
        step.setComment(comment);
        step.setSimulatedBy(getCurrentUsername());
        return step;
    }

    private boolean shouldUseDirectFinalOffer(PmReferenceTrain train) {
        return train.getProcessState() == PathProcessState.RECEIPT_CONFIRMED
                && !isDraftOfferAllowed(train);
    }

    private boolean isDraftOfferAllowed(PmReferenceTrain train) {
        PmTimetableYear year = train.getTimetableYear();
        if (year == null || year.getStartDate() == null) {
            return true;
        }
        return ttrPhaseResolver.isDraftOfferAllowed(year, LocalDate.now());
    }

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return ANONYMOUS_USER;
    }

    private PmReferenceTrain loadTrain(UUID referenceTrainId) {
        return referenceTrainRepository
                .findById(referenceTrainId)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Reference train not found: " + referenceTrainId));
    }

    private void validateTransition(
            PathProcessState currentState, PathAction action, PmReferenceTrain train) {
        Set<PathAction> allowed = ALLOWED_TRANSITIONS.get(currentState);
        boolean isAllowed = allowed != null && allowed.contains(action);

        if (!isAllowed && isDirectFinalOfferAllowed(currentState, action, train)) {
            isAllowed = true;
        }

        if (!isAllowed) {
            throw new IllegalStateException(
                    "Action "
                            + action
                            + " is not allowed in state "
                            + currentState
                            + ". Allowed: "
                            + (allowed != null ? allowed : "none"));
        }
    }

    private boolean isDirectFinalOfferAllowed(
            PathProcessState currentState, PathAction action, PmReferenceTrain train) {
        return currentState == PathProcessState.RECEIPT_CONFIRMED
                && action == PathAction.IM_FINAL_OFFER
                && !isDraftOfferAllowed(train);
    }

    private PathProcessState resolveTargetState(PathAction action) {
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
            copies.add(copyJourneyLocation(original, target));
        }
        target.getJourneyLocations().addAll(copies);
    }

    private PmJourneyLocation copyJourneyLocation(
            PmJourneyLocation source, PmTrainVersion targetVersion) {
        PmJourneyLocation copy = new PmJourneyLocation();
        copy.setTrainVersion(targetVersion);
        copy.setSequence(source.getSequence());
        copy.setCountryCodeIso(source.getCountryCodeIso());
        copy.setLocationPrimaryCode(source.getLocationPrimaryCode());
        copy.setPrimaryLocationName(source.getPrimaryLocationName());
        copy.setUopid(source.getUopid());
        copy.setJourneyLocationType(source.getJourneyLocationType());
        copy.setArrivalTime(source.getArrivalTime());
        copy.setArrivalOffset(source.getArrivalOffset());
        copy.setDepartureTime(source.getDepartureTime());
        copy.setDepartureOffset(source.getDepartureOffset());
        copy.setDwellTime(source.getDwellTime());
        copy.setArrivalQualifier(source.getArrivalQualifier());
        copy.setDepartureQualifier(source.getDepartureQualifier());
        copy.setSubsidiaryCode(source.getSubsidiaryCode());
        copy.setActivities(source.getActivities());
        copy.setAssociatedTrainOtn(source.getAssociatedTrainOtn());
        copy.setNetworkSpecificParams(source.getNetworkSpecificParams());
        copy.setTttPayload(source.getTttPayload());
        return copy;
    }
}
