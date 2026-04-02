package com.ordermgmt.railway.domain.pathmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ordermgmt.railway.domain.pathmanager.model.PathAction;
import com.ordermgmt.railway.domain.pathmanager.model.PathProcessState;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;
import com.ordermgmt.railway.domain.pathmanager.model.ProcessStepResult;
import com.ordermgmt.railway.domain.pathmanager.repository.PmProcessStepRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmReferenceTrainRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTrainVersionRepository;

@ExtendWith(MockitoExtension.class)
class PathProcessEngineTest {

    @Mock private PmReferenceTrainRepository referenceTrainRepository;
    @Mock private PmTrainVersionRepository trainVersionRepository;
    @Mock private PmProcessStepRepository processStepRepository;

    private PathProcessEngine engine;

    @BeforeEach
    void setUp() {
        engine =
                new PathProcessEngine(
                        referenceTrainRepository, trainVersionRepository, processStepRepository);
    }

    // ── getAvailableActions ───────────────────────────────────────────

    @Test
    void getAvailableActions_NEW_returnsSendRequest() {
        PmReferenceTrain train = trainInState(PathProcessState.NEW);
        when(referenceTrainRepository.findById(train.getId())).thenReturn(Optional.of(train));

        Set<PathAction> actions = engine.getAvailableActions(train.getId());

        assertThat(actions).containsExactly(PathAction.SEND_REQUEST);
    }

    @Test
    void getAvailableActions_CREATED_returnsExpected() {
        PmReferenceTrain train = trainInState(PathProcessState.CREATED);
        when(referenceTrainRepository.findById(train.getId())).thenReturn(Optional.of(train));

        Set<PathAction> actions = engine.getAvailableActions(train.getId());

        assertThat(actions)
                .containsExactlyInAnyOrder(
                        PathAction.MODIFY_REQUEST, PathAction.WITHDRAW, PathAction.IM_RECEIPT);
    }

    @Test
    void getAvailableActions_BOOKED_returnsExpected() {
        PmReferenceTrain train = trainInState(PathProcessState.BOOKED);
        when(referenceTrainRepository.findById(train.getId())).thenReturn(Optional.of(train));

        Set<PathAction> actions = engine.getAvailableActions(train.getId());

        assertThat(actions)
                .containsExactlyInAnyOrder(
                        PathAction.REQUEST_MODIFICATION,
                        PathAction.CANCEL_PATH,
                        PathAction.IM_ANNOUNCE_ALTERATION);
    }

    @Test
    void getAvailableActions_terminalState_returnsEmpty() {
        PmReferenceTrain train = trainInState(PathProcessState.WITHDRAWN);
        when(referenceTrainRepository.findById(train.getId())).thenReturn(Optional.of(train));

        Set<PathAction> actions = engine.getAvailableActions(train.getId());

        assertThat(actions).isEmpty();
    }

    @Test
    void getAvailableActions_DRAFT_OFFERED_returnsExpected() {
        PmReferenceTrain train = trainInState(PathProcessState.DRAFT_OFFERED);
        when(referenceTrainRepository.findById(train.getId())).thenReturn(Optional.of(train));

        Set<PathAction> actions = engine.getAvailableActions(train.getId());

        assertThat(actions)
                .containsExactlyInAnyOrder(
                        PathAction.REJECT_WITH_REVISION,
                        PathAction.REJECT_WITHOUT_REVISION,
                        PathAction.IM_FINAL_OFFER);
    }

    // ── executeTransition happy path ──────────────────────────────────

    @Test
    void executeTransition_NEW_SEND_REQUEST_movesToCREATED() {
        PmReferenceTrain train = trainInState(PathProcessState.NEW);
        when(referenceTrainRepository.findById(train.getId())).thenReturn(Optional.of(train));
        when(referenceTrainRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processStepRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProcessStepResult result =
                engine.executeTransition(train.getId(), PathAction.SEND_REQUEST, "initial request");

        assertThat(result.newState()).isEqualTo(PathProcessState.CREATED);
        assertThat(train.getProcessState()).isEqualTo(PathProcessState.CREATED);
        assertThat(result.processStep().getStepType()).isEqualTo("SEND_REQUEST");
        assertThat(result.processStep().getFromState()).isEqualTo("NEW");
        assertThat(result.processStep().getToState()).isEqualTo("CREATED");
        assertThat(result.newVersion()).isNull();
        verify(referenceTrainRepository).save(train);
    }

    @Test
    void executeTransition_CREATED_WITHDRAW_movesToWITHDRAWN() {
        PmReferenceTrain train = trainInState(PathProcessState.CREATED);
        when(referenceTrainRepository.findById(train.getId())).thenReturn(Optional.of(train));
        when(referenceTrainRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processStepRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProcessStepResult result =
                engine.executeTransition(train.getId(), PathAction.WITHDRAW, null);

        assertThat(result.newState()).isEqualTo(PathProcessState.WITHDRAWN);
    }

    @Test
    void executeTransition_CREATED_IM_RECEIPT_movesToRECEIPT_CONFIRMED() {
        PmReferenceTrain train = trainInState(PathProcessState.CREATED);
        when(referenceTrainRepository.findById(train.getId())).thenReturn(Optional.of(train));
        when(referenceTrainRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processStepRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProcessStepResult result =
                engine.executeTransition(train.getId(), PathAction.IM_RECEIPT, null);

        assertThat(result.newState()).isEqualTo(PathProcessState.RECEIPT_CONFIRMED);
    }

    @Test
    void executeTransition_FINAL_OFFERED_ACCEPT_OFFER_movesToBOOKED() {
        PmReferenceTrain train = trainInState(PathProcessState.FINAL_OFFERED);
        when(referenceTrainRepository.findById(train.getId())).thenReturn(Optional.of(train));
        when(referenceTrainRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processStepRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProcessStepResult result =
                engine.executeTransition(train.getId(), PathAction.ACCEPT_OFFER, "accepted");

        assertThat(result.newState()).isEqualTo(PathProcessState.BOOKED);
    }

    @Test
    void executeTransition_BOOKED_CANCEL_PATH_movesToCANCELED() {
        PmReferenceTrain train = trainInState(PathProcessState.BOOKED);
        when(referenceTrainRepository.findById(train.getId())).thenReturn(Optional.of(train));
        when(referenceTrainRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processStepRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProcessStepResult result =
                engine.executeTransition(train.getId(), PathAction.CANCEL_PATH, "no longer needed");

        assertThat(result.newState()).isEqualTo(PathProcessState.CANCELED);
    }

    // ── executeTransition with version creation ───────────────────────

    @Test
    void executeTransition_RECEIPT_CONFIRMED_IM_DRAFT_OFFER_createsNewVersion() {
        PmReferenceTrain train = trainInState(PathProcessState.RECEIPT_CONFIRMED);
        when(referenceTrainRepository.findById(train.getId())).thenReturn(Optional.of(train));
        when(referenceTrainRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(trainVersionRepository.findFirstByReferenceTrainIdOrderByVersionNumberDesc(
                        train.getId()))
                .thenReturn(Optional.empty());
        when(trainVersionRepository.save(any()))
                .thenAnswer(
                        inv -> {
                            PmTrainVersion v = inv.getArgument(0);
                            v.setId(UUID.randomUUID());
                            return v;
                        });
        when(processStepRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProcessStepResult result =
                engine.executeTransition(train.getId(), PathAction.IM_DRAFT_OFFER, "draft offer");

        assertThat(result.newState()).isEqualTo(PathProcessState.DRAFT_OFFERED);
        assertThat(result.newVersion()).isNotNull();
        assertThat(result.newVersion().getVersionNumber()).isEqualTo(1);
    }

    // ── executeTransition invalid transitions ─────────────────────────

    @Test
    void executeTransition_invalidAction_throws() {
        PmReferenceTrain train = trainInState(PathProcessState.NEW);
        when(referenceTrainRepository.findById(train.getId())).thenReturn(Optional.of(train));

        assertThatThrownBy(
                        () ->
                                engine.executeTransition(
                                        train.getId(), PathAction.ACCEPT_OFFER, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACCEPT_OFFER")
                .hasMessageContaining("NEW");
    }

    @Test
    void executeTransition_WITHDRAWN_noActionsAllowed_throws() {
        PmReferenceTrain train = trainInState(PathProcessState.WITHDRAWN);
        when(referenceTrainRepository.findById(train.getId())).thenReturn(Optional.of(train));

        assertThatThrownBy(
                        () ->
                                engine.executeTransition(
                                        train.getId(), PathAction.SEND_REQUEST, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void executeTransition_nonExistentTrain_throws() {
        UUID fakeId = UUID.randomUUID();
        when(referenceTrainRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> engine.executeTransition(fakeId, PathAction.SEND_REQUEST, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    // ── Helper ────────────────────────────────────────────────────────

    private PmReferenceTrain trainInState(PathProcessState state) {
        PmReferenceTrain train = new PmReferenceTrain();
        train.setId(UUID.randomUUID());
        train.setProcessState(state);
        train.setTridCompany("0085");
        train.setTridCore("12345");
        train.setTridVariant("01");
        train.setTridTimetableYear(2026);
        return train;
    }
}
