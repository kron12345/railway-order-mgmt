# ADR-011: Path Manager Architecture — Simulated TTT Communication

## Status
Accepted

## Context

The order management system needs to interact with an Infrastructure Manager (IM) for path (Trasse) ordering via the TTT (Train Timetable Transfer / TSI TAF/TAP) process. In the real production scenario, this communication happens between the Responsible Applicant (RA) and one or more IMs via standardized XML messages over a message broker.

During development and testing, however, the IM side does not exist as a separate system. We need a way to:

1. Develop and test the RA-side workflow (submit train, receive offers, accept/reject, book)
2. Simulate IM responses (receipt confirmation, draft offers, final offers, alterations)
3. Validate the data model and state machine before integrating with a real IM
4. Let users walk through the complete TTT lifecycle in the UI for training and acceptance testing

## Decision

We implement the Path Manager as a **separate bounded context within the same Spring Boot application**, exposed through a REST API at `/api/v1/pathmanager/**`. The REST API serves as the boundary between the order management side (RA) and the path management simulation (IM).

Key design decisions:

### REST API as Integration Boundary

The Path Manager exposes 11 REST endpoints across three controllers:
- `PathManagerController` — Train CRUD (submit, detail, list, update header, update journey location)
- `PathProcessController` — State machine operations (execute step, available actions, history)
- `PathManagerDiffController` — Version comparison

All endpoints are documented via Springdoc/OpenAPI and visible in Swagger UI. This means the API contract is explicit and machine-readable, even though the consumer is currently the same application.

### Shared Database with Separate Table Prefix

All Path Manager tables use the `pm_` prefix to clearly distinguish them from order management tables:
- `pm_timetable_years`, `pm_reference_trains`, `pm_train_versions`
- `pm_journey_locations`, `pm_routes`, `pm_path_requests`
- `pm_paths`, `pm_process_steps`

The only cross-boundary link is `order_positions.pm_reference_train_id`, which references `pm_reference_trains.id`. Data flows in one direction: from order positions to PM reference trains (copied on submission), never the reverse.

### Static State Machine

The `PathProcessEngine` implements the TTT lifecycle as a static `EnumMap<PathProcessState, Set<PathAction>>` transition table. This avoids dependencies on external state machine libraries while keeping the logic transparent and testable. The engine also handles version creation: certain IM actions (draft offer, final offer, alteration offer) automatically clone the latest train version with all journey locations.

### Simulation Approach

Both RA and IM actions are available in the UI. The user can simulate the full conversation:
- RA actions: SEND_REQUEST, MODIFY_REQUEST, WITHDRAW, ACCEPT_OFFER, REJECT_WITH_REVISION, etc.
- IM actions: IM_RECEIPT, IM_DRAFT_OFFER, IM_FINAL_OFFER, IM_NO_ALTERNATIVE, IM_BOOK, etc.

In production, IM actions would be triggered by incoming TTT messages, not by user clicks. The current simulation mode allows testing the complete flow without external dependencies.

## Alternatives Considered

### A. Separate Microservice

A standalone microservice for the Path Manager with its own database and inter-service communication (REST or messaging).

**Rejected because:** The overhead of a separate deployment, database, and inter-service communication is not justified at this stage. The domain model is still evolving, and having everything in one application allows faster iteration. The REST API boundary already provides the right level of decoupling — extracting to a separate service later would be straightforward.

### B. Direct Service Injection

Calling `PathManagerService` directly from `OrderPositionService` without a REST API layer.

**Rejected because:** This would create tight coupling between bounded contexts and make it impossible to extract the Path Manager later. More importantly, it would not provide a realistic API contract for testing, and Swagger documentation would not be available.

### C. Message Queue (RabbitMQ / Kafka)

Implementing the TTT communication via an async message queue, closer to the real production architecture.

**Rejected because:** Asynchronous messaging adds complexity (message serialization, error handling, retry logic, dead letter queues) that is not needed for simulation purposes. The synchronous REST API is simpler to debug and test. If message-based integration is needed later, the REST endpoints can be wrapped with message consumers without changing the domain logic.

## Consequences

### Positive

- **Realistic API contract**: The REST endpoints define the exact interface that a real IM integration would use. The Swagger documentation serves as a living specification.
- **Clean bounded context boundary**: The `pm_` table prefix and REST API layer enforce separation. Domain objects are not shared across contexts.
- **Testable state machine**: The static transition table is easy to unit test. Every allowed transition and every error path is explicit.
- **Incremental migration path**: When a real IM is available, the `PathProcessController` endpoints for IM actions can be replaced by incoming message handlers while the domain logic remains unchanged.
- **User training**: The simulation mode lets users learn the TTT workflow before the real system is available.

### Negative

- **Shared database risk**: Both bounded contexts share the same PostgreSQL instance. Schema evolution must be coordinated. The `pm_reference_train_id` FK couples the two schemas.
- **Simulated actions are unrealistic**: IM responses in production would include modified train data in the TTT message payload. The current simulation creates empty version copies that the user must manually edit.
- **No message-level validation**: The real TTT protocol has strict XML schema validation. The REST DTOs are simpler and do not enforce all TTT constraints.

### Migration to Production

When integrating with a real IM, the following changes would be needed:

1. Replace `PathProcessController` IM endpoints with TTT message consumers
2. Parse incoming TTT XML messages into `PmTrainVersion` + `PmJourneyLocation` entities
3. Add outbound message generation for RA actions (SEND_REQUEST, MODIFY_REQUEST, etc.)
4. Keep the `PathManagerController` endpoints for internal UI consumption
5. The `PathProcessEngine` state machine logic remains unchanged
