# ADR 0010: Enrollment Clock Input and Time-Trust Boundary

- Status: Proposed; source audit only
- Date: 2026-11-03
- Scope: v1 device-enrollment proof receipt, owner-decision import, and owner-side finalization
- Related: ADR 0006 (time/text semantics), ADR 0007 (cross-device lifecycle), ADR 0008 (owner decision persistence), ADR 0009 (Room migration and instrumentation)

## Context

Enrollment verification uses epoch-millisecond timestamps, but the current service APIs accept `nowEpochMillis` as an ordinary caller-supplied value. The current Compose screen supplies `System.currentTimeMillis().coerceAtLeast(0L)`. This is a useful test seam and reflects the device wall clock; it is not a cryptographically authenticated or rollback-resistant time source.

This ADR records the actual trust boundary and defines requirements for any future expiry-policy implementation. It does not select the expiry policy from ADR 0006 and does not authorize runtime changes.

## Source audit (main branch, 2026-11-03)

1. `core/identity/.../DeviceEnrollmentProofAcceptanceService.kt`
   - `accept(proof, identity, nowEpochMillis)` receives time as a parameter.
   - The supplied value is passed to `DeviceEnrollmentProofVerifier.verify` and then stored as `PendingEnrollmentRequest.receivedAtEpochMillis`.
2. `core/identity/.../EnrollmentDecisionAcceptanceService.kt`
   - `accept(decision, nowEpochMillis)` receives time as a parameter.
   - The value is passed to `EnrollmentDecisionVerifier.verify` and persisted as `recordedAtEpochMillis`.
3. `core/identity/.../EnrollmentFinalizationService.kt`
   - `finalize(acceptance, nowEpochMillis)` receives time as a parameter.
   - The value is passed to `EnrollmentAcceptanceVerifier.verify`.
   - The service does not independently read a clock or establish that the supplied value came from a trusted source.
4. `app/src/main/java/com/familymesh/app/ui/FamilyMeshHomeScreen.kt`
   - Current proof receipt, decision import, and finalization call sites pass `System.currentTimeMillis().coerceAtLeast(0L)`.
   - Owner inbox enablement also compares `System.currentTimeMillis()` with request expiry.
   - Owner decision signing and applicant acceptance signing similarly use the device wall clock.
5. `core/common/.../EnrollmentFinalizationRepository.kt` and `core/storage/.../RoomEnrollmentFinalizationRepository.kt`
   - The repository accepts the verified device record and persists its signed event time (`joinedAtEpochMillis`); it does not establish receipt time or enforce an expiry policy.

The current call sites are therefore not arbitrary user text inputs, but they still rely on a mutable device wall clock and a caller-controlled service parameter. The service signature alone cannot prove that its time argument is fresh, monotonic, or obtained at the point of receipt.

## Security and correctness implications

- A wall-clock rollback or forward adjustment can affect local expiry decisions and audit timestamps.
- Clamping negative wall-clock values to zero prevents negative values from reaching the services; it does not make the clock trustworthy.
- A caller can technically invoke the public service API with a different nonnegative timestamp. Current UI call sites do not do so, but the service boundary does not prevent it.
- Signed event time (`acceptedAtEpochMillis`) and owner-local observation time are different facts. Neither should be overwritten with the other.
- A monotonic clock is useful for measuring elapsed time within one boot/session, but it cannot directly replace Unix epoch time for cross-device signed events.
- Android wall-clock time is not a trusted timestamping authority. Do not describe it as tamper-proof or rollback-resistant.

## Requirements before implementation

1. Resolve ADR 0006's product/security choice: owner receipt must occur before expiry, or a valid signed acceptance created before expiry may be delivered later.
2. Keep signed event timestamps separate from local observation/recording timestamps.
3. Centralize time acquisition behind an explicit clock abstraction at the application/service boundary; do not let UI call sites independently manufacture security-sensitive `now` values.
4. Make the source of local time explicit in types/API documentation. A clock abstraction improves consistency and testability; it does not itself provide tamper resistance.
5. If rollback resistance is a hard requirement, specify a defensible threat model and platform mechanism before claiming it. A plain `Clock` wrapper around `System.currentTimeMillis()` is not a security control.
6. Do not allow external callers to choose the owner-local receipt timestamp in production. Capture it at the trusted owner-side boundary and pass it as an internal verified observation.
7. Add tests for negative values, exact expiry boundary, clock rollback/forward movement, delayed offline delivery, and separation of signed event time from local receipt/recording time.
8. Do not implement expiry cleanup or change finalization semantics until ADR 0006 is accepted.

## Decision

No runtime or database changes are made by this audit. The current v1 behavior remains unchanged. ADR 0006's expiry policy remains unresolved. This ADR is a source-backed record of the present clock trust boundary and a checklist for a future implementation decision.
