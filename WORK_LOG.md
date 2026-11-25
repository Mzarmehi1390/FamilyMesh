# FamilyMesh Work Log

## 2026-11-03 — Repository synchronization and documentation correction

### Baseline

- Repository: `Mzarmehi1390/FamilyMesh`
- Branch: `main`
- Baseline commit: `5d1afe394178ead84d8f54219e28ee3ccf7c1675`
- Latest CI run at baseline: #41667652277 — success.

### Verified

- Enumerated the complete repository tree (146 entries).
- Inspected module structure, Gradle configuration, Android manifest,
  workflow, roadmap, architecture docs, identity/enrollment services,
  Room persistence and migrations, and home-screen enrollment flow.
- Confirmed no open issues or pull requests at synchronization.
- Confirmed the latest CI run passed unit tests, debug APK assembly, and
  artifact upload.

### Findings

- The code had advanced beyond README, roadmap, architecture, and membership
  documentation.
- The current signed enrollment payloads are custom fixed-order,
  length-prefixed v1 encodings; ADR 0001's COSE ESP256 profile remains
  proposed, not implemented.
- Owner-side authenticated-device records are local only and are not yet
  consumed by a production authorization resolver.
- No E2EE messaging, P2P transport, relay, revocation, or recovery exists.
- Gradle Wrapper and Android instrumentation validation remain outstanding.

### Documentation changes

- Corrected README and architecture/roadmap status to match implementation.
- Documented applicant acceptance and owner-side finalization.
- Added project status and handover records.
- Recorded protocol review as the next work package.

### Tests

No tests were executed by this documentation-only synchronization. The
reported passing tests are from the already completed GitHub Actions run
#41667652277 for the baseline commit.

### Next action

Audit the signed enrollment chain and document a reviewed protocol decision
before implementing authorization integration or network transport.

## 2026-11-03 — Close owner-pin trust-boundary gap

### Finding

`EnrollmentDecisionAcceptanceService` previously accepted a
`TrustedInitialOwner` supplied by its caller. The current UI checked
the pin before calling the service, but the service boundary itself did not
guarantee that the supplied owner key came from the local pin repository.

### Change

- Injected `InitialOwnerPinRepository` into the acceptance service.
- The service now resolves exactly one pin for the decision's family before
  checking or persisting the decision.
- Removed the caller-supplied trust parameter from the service API and UI.
- Updated Koin wiring.
- Added three tests: missing pin, different pinned key, and ambiguous pins.

### Verification

- Commit: `60f02eea4abe2d6ff8d9fe1d4c404724f049c1a8`
- CI run: [#41670679347](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41670679347)
- Unit tests: passed.
- Debug APK assembly: passed.
- Artifact upload: passed.

### Remaining limitation

This closes the caller-injected trust-record gap. It does not protect against
compromise or arbitrary modification of the local application database and
does not replace an independently reviewed trust protocol.

## 2026-11-03 — Enrollment protocol boundary hardening

### Canonical transfer encoding

Audit confirmed that all four text envelope decoders used Java's URL-safe
Base64 decoder directly. The encoders emitted unpadded Base64URL, but the
decoders did not enforce that single representation. Added
`CanonicalBase64Url`, which checks the URL-safe alphabet, rejects padding,
decodes, and round-trips to require exact canonical representation. Applied
it to FMER1, FMOT1, FMOD1, and FMJA1. Added tests for canonical input,
padded aliases, and non-URL-safe alphabet input.

### Public-key algorithm constraints

The Android Keystore generator creates P-256 keys, and ADR 0001 proposes
P-256/SHA-256, but the verification paths previously accepted any EC curve
supported by the JCA provider. Added `P256PublicKeyParser` and applied it to
proof, owner trust bundle, owner decision, and applicant acceptance
verification. Added P-384 rejection and P-256 acceptance tests.

### Verification

- Canonical Base64URL commit: `918b03b75181f6aa541e5df296cc6ad8164c84e6`
  — CI #41671671596 passed.
- P-256 verification commit: `36e2d27c0fcac8defe36a116fdd6792e071f011a`
  — CI #41671944679 passed.
- Parser block-body cleanup: `21dd7ca4db718ee31416e45319d88ffce9c950c3`
  — CI #41671995919 passed unit tests, APK assembly, and artifact upload.

### Remaining protocol decisions

COSE_Key serialization, canonical key ID, DER-to-COSE signature conversion,
low-S policy, cross-language vectors, maximum request lifetime, clock skew,
distributed replay/freshness, trust ceremony, revocation, and recovery remain
open. The local custom format is still not a production protocol.

## 2026-11-03 — Protocol profile research and handover refresh

### Standards review

- Confirmed RFC 9864 (October 2025) registers ESP256 as COSE algorithm -9,
  fully specifying ECDSA with P-256 and SHA-256, and marks polymorphic COSE
  ES256 (-7) deprecated.
- Confirmed RFC 9053 encodes ECDSA signatures as fixed-width R || S, each
  coordinate padded to the curve byte width (32 bytes for P-256).
- Confirmed RFC 9052 COSE_Sign1 signs the Sig_structure with context
  "Signature1", protected-header bytes, external AAD, and payload.
- Confirmed RFC 9053 requires EC2/P-256 key type/curve consistency.
- Confirmed RFC 8949 core deterministic CBOR uses preferred serialization,
  forbids indefinite-length items, and sorts map keys by bytewise
  lexicographic order of deterministic encodings.

### Documentation work

- Updated ADR 0001 to cite RFC 9864 and distinguish ESP256 from deprecated
  polymorphic ES256.
- Added ADR 0003 as a draft v2 COSE profile, with explicit proposals for
  key encoding, key IDs, protected headers, external AAD, signature
  conversion, and migration boundaries.
- Refreshed roadmap, handover, and project status to reflect the verified
  CI state and current source/documentation distinction.
- Corrected the stale literal "\\n" formatting defect in the roadmap.

### Verification

Documentation changes were committed as a cohesive update in
`be4a85cd7d2b357e3fb356c2834d94491c59db95`. CI must pass on that commit
before this documentation package is considered verified. No cryptographic protocol implementation or authorization/
networking behavior was changed in this work package.


## 2026-11-03 — COSE v2 primitive known-answer fixture

### Work completed

- Added `docs/architecture/decisions/vectors/cose-v2-primitive-kat-0001.md`.
- Fixture covers deterministic CBOR COSE_Key bytes, v2 key-ID derivation,
  protected headers, artifact-specific external AAD, COSE Sig_structure,
  SHA-256 digest, P-256 ECDSA R||S, proposed low-S normalization, and complete
  tagged COSE_Sign1 encoding.
- Uses the deliberately public private scalar 1 only as a reproducible
  mathematical test fixture; explicitly prohibited for any device identity.
- Generated an RFC 6979 HMAC-SHA-256 deterministic signature and verified it
  against the exact Sig_structure using an independent cryptographic library.

### Verification limits

This is one primitive vector, not the four final artifact vectors and not
cross-implementation interoperability evidence. No Android source code or
runtime behavior was changed. The next gates remain independent reproduction,
negative-vector corpus, Android Keystore instrumentation, and final review of
payload/time/replay/authority lifecycle semantics.

### Commit

- Vector fixture commit: `ba150159ca8b1bb10412345caa0fd78b596bdfe8`.
- Documentation synchronization and CI verification are pending.


## 2026-11-03 — COSE v2 schema parity review

### Work completed

- Added ADR 0004 to compare all four v1 signed artifacts with the draft v2
  requirements before assigning CBOR labels.
- Identified a concrete parity omission: v1 ApplicantAcceptance carries the
  applicant public key, while the initial ADR 0003 acceptance field list did
  not mention it.
- Updated ADR 0003 to require explicit review and preservation of the
  applicant public COSE_Key unless a separately reviewed decision removes it.
- Recorded that exact-decision embedding and digest binding need a deliberate
  choice; a digest cannot be introduced without fixing its algorithm and
  canonical input bytes.

### Verification limits

No Kotlin source, runtime, persistence schema, transport, or authorization
behavior changed. The current GitHub combined-status endpoint reports no
status entries for the latest documentation commit; CI completion is
therefore not confirmed. The schema remains unapproved pending direct source
model/codec inspection and independent vectors.

### Commits

- ADR 0004: 10e493a6aec11032a8110c3a9c548d15d2c637fe
- ADR 0003 parity correction: 63c722224ad1d94244b8824a38c1b3ec1adaa7c9


## 2026-11-03 — Source-backed v1 enrollment field inventory

### Work completed

- Inspected the actual Kotlin models and transfer codecs for all four
  enrollment artifacts, plus DevicePublicIdentity and FamilyRole.
- Added ADR 0005 with source-backed fields, current constraints, binary/text
  envelope caps, signed-vs-transfer-only metadata, and migration observations.
- Confirmed that FMER1 carries identity creation time and X.509 public key
  outside the proof's signed semantic payload; proof nonce is exactly 32 bytes.
- Confirmed FMOT1 signs owner public-key bytes; FMOD1 binds family/request/
  applicant/owner/outcome/time; FMJA1 signs the exact encoded FMOD1 decision,
  applicant key ID, applicant public key, and acceptance time.
- Recorded the mismatch between String character limits and UTF-8 byte
  limits, and that all current timestamps are epoch milliseconds with only
  non-negative checks and no upper bound.

### Verification limits

No runtime or wire implementation changed. The inventory is documentation
based on the inspected main-branch Kotlin sources; it does not approve CBOR
labels, a v2 time policy, or a key migration. CI status for the latest commit
has not yet been confirmed.

### Commit

- ADR 0005: f784a42afc29c57b8d9ac829ab5755cd998d7521


## 2026-11-03 — v2 time and text semantics proposal

### Work completed

- Re-inspected v1 applicant proof signing/verification, acceptance verification,
  codec UTF-8 handling, domain request constraints, and Room nonce consumption.
- Added ADR 0006 as a proposal covering epoch-millisecond bounds, validity
  interval semantics, decision/acceptance ordering, strict Unicode encoding,
  byte-based limits, and identifier-profile review.
- Recorded an encoder-side Unicode edge case: malformed UTF-16 input is not
  explicitly rejected before Kotlin UTF-8 conversion, even though decoders
  reject malformed UTF-8.
- Kept numeric TTL/skew values and all protocol schema choices unapproved
  pending product/security review and identifier-generator inventory.

### Verification limits

Documentation only; no runtime or v1 behavior changed. No local tests were
run. CI for this and subsequent documentation commits remains unconfirmed
until a matching workflow result is observed.

### Commit

- ADR 0006: ba7543def504bd7157cd1617c9fb96b5a2e730ae


## 2026-11-03 — Signed event time versus local receipt time

### Work completed

- Inspected proof acceptance, nonce persistence, owner decision import, and
  owner-side finalization services against the v1 verifiers.
- Confirmed that `receivedAtEpochMillis` is assigned by the receiving
  installation and atomically accompanies local nonce consumption; it is not
  covered by the applicant signature.
- Confirmed owner decision import separately records local
  `recordedAtEpochMillis`.
- Identified an important v1 lifecycle behavior: decision and acceptance
  signed timestamps must fall within the request interval and not exceed
  verifier time, but the verifier does not require the current receipt time
  itself to be before expiry. Thus an event signed before expiry can be
  delivered after expiry while its pending record remains.
- Updated ADR 0006 to require an explicit v2 choice between event-time expiry
  and receipt-time expiry, and to preserve local receipt metadata as distinct
  from signed event claims.

### Verification limits

Documentation only. No runtime behavior changed; no local tests were run.
CI for the documentation commit is unconfirmed.

### Commit

- ADR 0006 clarification: ec33dae03dd6d5e331a8af69fb67e317b88240c2


## 2026-11-03 — Pending-request expiry lifecycle audit

### Work completed

- Inspected the Room DAO transaction implementations, not just the domain
  services.
- Confirmed expired nonce rows are pruned during new nonce consumption, but
  pending enrollment rows have no corresponding expiry cleanup query.
- Confirmed owner finalization validates the applicant-signed acceptance
  timestamp against request expiry, but does not compare the actual receive
  time to expiry. A still-pending owner-side row can therefore remain
  finalizable after expiry if the acceptance claims an in-window timestamp.
- Extended ADR 0006 with this persistence-level lifecycle gap and explicit
  requirements for pending-row expiration and post-expiry eligibility tests.

### Verification limits

This is a source audit and documentation change, not a runtime fix. No local
tests were run. CI for these documentation commits remains unconfirmed.

### Commit

- ADR 0006 lifecycle clarification: d8cd41c265e0602e164da7a39f1ec5e184d6994d


## 2026-11-03 — Cross-device enrollment lifecycle audit

### Work completed

- Traced the actual Compose owner/applicant actions, decision and acceptance
  services, repository implementations, Room transactions, and unit tests.
- Resolved an apparent contradiction: applicant-side FMOD1 import deletes
  only the applicant installation's pending row; owner-side FMJA1 finalization
  uses a separate owner-local pending row. The two stores do not synchronize.
- Found a concrete owner-inbox lifecycle gap: issuing FMOD1 does not persist
  an owner-side decision or update the owner pending row. Rejected requests
  therefore remain displayed as awaiting owner review; approved requests
  also remain indistinguishable from unreviewed requests until finalization.
- Confirmed pending-row expiry cleanup is absent, while nonce cleanup exists.
- Added ADR 0007 with the cross-device state trace, source-backed gap,
  design constraints, and required integration/instrumentation test matrix.

### Verification limits

Documentation-only audit. No Kotlin, Room schema, UI, or runtime behavior
changed. No local tests were run. The current CI workflow does not run Android
instrumentation tests. CI for this documentation commit must be checked
separately and is not assumed to have passed.


## 2026-11-03 — Owner-side decision persistence design

- Inspected the current Room schema and repository semantics. The existing
  `enrollment_decisions` table is used by applicant-side decision acceptance;
  it is not an owner-side issuance ledger.
- Confirmed the owner Compose action signs FMOD1 into transient UI state and
  has no durable owner-side issuance operation.
- Added ADR 0008 proposing a separate owner-issued decision ledger, explicit
  inbox lifecycle states, exact-artifact retention, atomic persistence,
  conflict/replay behavior, and fail-closed matching during FMJA1 finalization.
- Expiry policy remains deliberately unresolved per ADR 0006. No runtime or
  schema changes were made pending review of the state and timing semantics.
- No local tests were run; CI status is checked independently.


## 2026-11-03 — Room migration and instrumentation coverage plan

### Work completed

- Re-inspected `FamilyMeshDatabase`, its entity/DAO definitions, all
  registered migrations, the app instrumentation configuration, storage
  module dependencies, and the GitHub Actions workflow.
- Confirmed the database is schema version 7 with a continuous migration
  chain from 1 → 7.
- Confirmed CI currently runs JVM unit tests and debug APK assembly only;
  it does not execute Android instrumentation tests. Room schema export is
  disabled, and the storage module has no dedicated migration-test setup.
- Added ADR 0009 with an additive 7 → 8 migration strategy for a future
  owner-issued decision ledger, historical migration fixture requirements,
  SQLite transaction/rollback/concurrency coverage, and a separate emulator
  CI job proposal.
- Kept this as a test/design plan: no database schema, runtime, expiry
  semantics, or cryptographic behavior changed.

### Verification

- ADR 0009 commit: `96e902f4b869b279dad7928a2e57b92688bfe439`.
- Prior ADR 0008, Work Log, and Project Status CI runs were checked and
  completed successfully.
- No local tests were run for this documentation-only change. CI for ADR
  0009 must be checked separately; the existing workflow does not execute
  instrumentation tests.

### Next action

Resolve the expiry policy in ADR 0006 and review ADR 0008's owner-ledger
semantics before implementing schema version 8. In parallel, add the
instrumentation test infrastructure as a separately reviewable CI change.


## 2026-11-03 — Expiry semantics review clarification

### Work completed

- Corrected the malformed literal newline sequences in ADR 0006's T3
  signed-event versus local-receipt section.
- Added a structured comparison of owner-receipt-deadline and signed-event-
  deadline policies, including offline-delivery tradeoffs, clock assumptions,
  required product/security inputs, and implementation invariants.
- Explicitly kept numeric TTL, skew, grace period, cleanup, and EXPIRED-state
  behavior undecided. No v1 runtime behavior was changed.

### Verification

- ADR 0006 clarification commit: `e72009183eaebf8bbcf75ebb0af3828b882899c0`.
- The ADR 0009 and Work Log CI runs completed successfully:
  #41678430461 and #41678451624.
- No local tests were run; this is a documentation-only change.
- CI for the ADR 0006 clarification must be checked separately.

### Next action

Obtain product/security answers to the five questions in ADR 0006 before
selecting expiry semantics. Then implement owner-ledger persistence and
Room instrumentation in separately reviewable changes.


## 2026-11-03 — Enrollment clock trust-boundary audit

### Work completed

- Added ADR 0010 after tracing all three time-accepting enrollment services to their current Compose call sites.
- Confirmed proof receipt, owner-decision import, and owner-side finalization receive `System.currentTimeMillis().coerceAtLeast(0L)` from the UI.
- Confirmed the service APIs themselves accept caller-supplied `nowEpochMillis`; they do not independently acquire or authenticate the time source.
- Distinguished this caller-controlled wall-clock boundary from signed event timestamps and local receipt/recording timestamps.
- Documented clock rollback/forward implications and implementation/test requirements without changing runtime behavior.

### Verification

- ADR 0006 clarification CI #41680089589: success.
- Work Log CI #41680127933: success.
- ADR 0010 commit: `0d7271217f308d7f854555bc7b5871b5628550a8`.
- No runtime or database changes; no local tests run for this documentation-only audit.

### Next action

Resolve ADR 0006's expiry policy and threat model before centralizing production clock acquisition or changing finalization behavior. Review ADR 0008/0009 before any owner-ledger schema implementation.


## 2026-11-03 — Finalization replay exactness audit

### Work completed

- Added ADR 0011 after tracing the already-finalized fast path through the identity service and Room repository.
- Confirmed the fast path returns `ALREADY_FINALIZED` from applicant key ID plus stored acceptance-signature bytes, before re-verifying the complete FMJA1 artifact.
- Confirmed the current stored membership record does not retain the exact FMJA1 artifact or a digest of its complete signed payload.
- Identified a result-integrity gap: a modified envelope retaining the same key ID and signature bytes can be reported as an already-finalized replay. This does not create a second membership row, but it is not proof of exact replay.
- Specified additive replay-identity persistence, legacy-row behavior, and unit/Room test coverage. No runtime or schema changes were made.

### Verification

- ADR 0011 commit: `ede1f826b2a612e32173dc8050b34f7ea6766770`.
- Documentation-only change; CI pending. No local tests run.

### Next action

Review the exact-replay compatibility policy for existing finalized rows, then coordinate any replay-identity column with ADR 0008/0009's owner-ledger migration. Keep expiry-policy work separate until ADR 0006 is accepted.


## 2026-11-03 — Unified Room v7-to-v8 enrollment persistence design

### Work completed

- Added ADR 0012 to coordinate the proposed owner-issued decision ledger and exact FMJA1 replay identity in one additive Room 7 → 8 migration.
- Verified the FMJA1 codec deterministically re-encodes decoded acceptances and rejects non-canonical Base64URL and trailing payload bytes.
- Specified a domain-separated SHA-256 digest over the complete canonical FMJA1 text, including nested FMOD1, applicant key identity/public key, accepted time, and signature.
- Specified NULL replay digests for migrated v7 rows; legacy artifacts cannot be reconstructed from the fields currently stored and must not be represented as exact replays.
- Linked ADR 0012 from ADR 0009. No runtime, schema, protocol, or expiry-policy changes were made.

### Verification

- Source audit covered `EnrollmentFinalizationService`, `RoomEnrollmentFinalizationRepository`, `FamilyMeshDatabase`, `EnrollmentFinalizationRepository`, and `EnrollmentAcceptance`.
- ADR 0011's four Android CI runs completed successfully. ADR 0012 and this log update require their own CI runs.
- Documentation-only changes; no local tests run.

### Next action

Review/approve ADR 0008 owner-ledger semantics and ADR 0012 migration/legacy replay behavior before any schema implementation. Keep ADR 0006 expiry policy separate.


## 2026-11-03 — Owner decision issuance consistency audit

### Work completed

- Added ADR 0013 after tracing the owner Compose approve/reject actions, applicant-side decision acceptance transaction, and owner-side FMJA1 finalization service.
- Confirmed owner issuance currently signs and displays FMOD1 in transient Compose state without persisting an authoritative decision.
- Confirmed the owner can issue distinct opposite outcomes for the same still-pending request; finalization currently validates the embedded owner signature and request binding but does not compare FMOD1 with an owner-issued ledger.
- Identified the resulting consistency gap: a previously issued valid approval can still finalize while the owner request remains pending even if a later rejection was also signed. This is not a signature forgery; it is missing authoritative-decision state.
- Confirmed applicant-side decision import deletes only the applicant installation's pending row in its own transaction and does not synchronize owner-side state.
- Specified one-authoritative-artifact, idempotent re-export, conflict-on-different-issuance, exact-ledger-match-at-finalization, restart, failure, and concurrency invariants.
- No runtime, Room schema, protocol, cryptographic, or transport changes were made.

### Verification

- Reviewed `FamilyMeshHomeScreen.kt`, `EnrollmentDecisionAcceptanceService.kt`, `RoomEnrollmentDecisionRepository.kt`, `EnrollmentFinalizationService.kt`, `RoomEnrollmentFinalizationRepository.kt`, and the Room DAO transaction definitions.
- Prior ADR 0012 / ADR 0009 / status-document CI runs all succeeded. ADR 0013 and follow-up documentation CI pending.
- Documentation-only audit; no local tests run.

### Next action

Review ADR 0013 together with ADR 0008 and ADR 0012. Implement only after approval of the owner-ledger invariant and the ADR 0006 expiry policy. The implementation must enforce the invariant in Room transactions, not only in Compose UI.


## 2026-11-03 — Enrollment persistence test-coverage audit

### Work completed

- Added ADR 0014 after inspecting the current Android CI workflow, the complete repository test-source tree, identity service tests, Room DAO transactions, and storage module dependencies.
- Confirmed current CI runs unit tests and APK assembly only; it does not execute Android instrumentation or Room migration tests.
- Confirmed `:core:storage` has no test/androidTest source sets or test dependencies, and Room schema export is disabled.
- Confirmed enrollment service tests use fake repositories, so they do not validate actual SQLite rollback, uniqueness races, foreign keys, or close/reopen durability.
- Recorded a staged validation plan covering Room DAO tests, historical migration fixtures, emulator instrumentation, CI reporting, and the future ADR 0008/0012 ledger/replay cases.
- No test code, CI, runtime, schema, protocol, cryptographic, or transport changes were made.

### Verification

- Five CI runs for ADR 0013 and its coordinated documentation updates (#41689232031, #41689300728, #41689312246, #41689319946, #41689327795) completed successfully.
- ADR 0014 is documentation-only; its CI and the follow-up status-document CI runs must be checked.

### Next action

Add real Room/SQLite test infrastructure only after reviewing ADR 0014's scope and agreeing on the required Android test environment. Keep the owner-ledger implementation gated on ADR 0008/0012 approval and ADR 0006 expiry-policy resolution.


### Follow-up finding — concurrent applicant decision import

- A second pass over `EnrollmentDecisionAcceptanceService` and `EnrollmentNonceDao.persistDecision` found that the DAO's existing-row comparison includes local `recordedAtEpochMillis`, while the service's sequential replay comparison does not.
- Concurrent imports of the same signed FMOD1 with different local receipt times may therefore yield one record and one conflict. This race is not covered by current tests.
- Added the required concurrency case and design question to ADR 0014. No runtime behavior changed.


## 2026-11-03 — Initial Room DAO instrumentation baseline

### Work completed

- Added Android instrumentation configuration to `:core:storage` using the repository's existing JUnit and AndroidX test catalog entries.
- Added SQLite-backed Room instrumentation tests for nonce replay rejection, applicant decision persistence/idempotent replay/conflict, and owner finalization/idempotent replay/conflict.
- Added a dedicated emulator-backed `storage-instrumentation` CI job, separate from unit tests and APK assembly, with reports uploaded even on failure.
- The tests use an in-memory Room database and exercise the actual DAO transaction implementations rather than fake repositories.
- This is an initial transaction baseline only; historical migration fixtures, injected rollback failures, concurrent import races, and close/reopen durability remain outstanding.
- No production runtime, database schema, protocol, cryptographic, or transport behavior changed.

### Verification

- Storage Gradle configuration commit: `3b38a2a1a685cc77324520cf9b010614039f440a`.
- Instrumentation CI workflow commit: `e94f00e98443780c55e2e15959c94fc07b0bf86f`.
- DAO instrumentation tests commit: `5868c4a098707548321aee53519dec8fe6efad10`.
- CI runs #41691001137, #41691009090, and #41691021028 were queued/in progress at the time of this log update.

### Next action

Resolve any compile/emulator failures from the first instrumentation run. Then add historical migration fixtures and deterministic race/rollback/reopen tests before considering the suite sufficient for the ADR 0008/0012 schema work.


### Follow-up test expansion

- Extended the Room instrumentation suite with a transaction rollback case: a duplicate pending-request insert must roll back the preceding nonce insert, allowing the same nonce digest to be retried for a different request.
- Added a file-backed close/reopen test verifying pending enrollment and nonce replay state survive database recreation.
- Follow-up test commits: `0b620981aee5fc75388f5725c209c31c6ac75d68` and cleanup `b100e6e4d8ac550bdc1659ae15b136749f17a832`. The corresponding CI run is pending.


## 2026-11-04 — API 34 instrumentation failure triage and status correction

### CI evidence

- Latest source commit: `e945b8774d8d43d8426e8f2c2f4f541800d1d2cb`.
- Run #41697147520: `verify` passed, including JVM unit tests, debug APK assembly, and APK artifact upload.
- `storage-instrumentation` failed before the Gradle command ran. API 34 / Google APIs / x86_64 / Pixel 2 / SwiftShader did not reach `sys.boot_completed=1`; the action timed out after its 600-second boot window.
- The runner log repeatedly reports ADB exit code 1 while polling the emulator. No connected Android test report exists, so this run gives no evidence about the six Room tests themselves.
- Earlier API 34 run #41696869594 failed at the same boot boundary. Run #41697076341 was cancelled as a newer push started; this does not establish a failure of the concurrency configuration.

### Test inventory correction

The instrumentation source now contains six DAO tests. The sixth test, `sqliteConstraints_enforceDeviceUniquenessAndFamilyCascade`, exercises the unique authenticated-device key constraint and family-delete cascade for pending and authenticated-device rows. It remains unexecuted due to emulator infrastructure failure.

### Documentation

- Updated `PROJECT_STATUS.md` to distinguish successful JVM/build verification from unverified emulator instrumentation.
- Updated ADR 0014 with the API 34 failure evidence and the six-test inventory.
- No production runtime, Room schema, protocol, crypto, or transport behavior changed.

### Next action

Resolve emulator startup independently of test assertions, then run `:core:storage:connectedDebugAndroidTest` and inspect the actual SQLite test results. Do not mark instrumentation as passed until a completed run executes all six tests.


## 2026-11-04 — Room instrumentation passes and schema export recovered

- Run #41813611838 / #172 at commit `668542632261dac50d98915e76765a5125a4c76f` passed both jobs. The emulator report confirms all six DAO instrumentation tests executed: 6/6, 0 skipped, 0 failed.
- The prior run failed because the instrumentation APK did not include `androidx.test.runner.AndroidJUnitRunner`. Added the explicit AndroidX runner dependency to the version catalog and storage module.
- Run #41818762325 / #173 at commit `da5e86a94937fcbbc9d0a221b8a2d0c091a50358` successfully completed unit tests, APK assembly, explicit Room KSP schema generation, and schema artifact upload. Instrumentation was still running at the time of this log update.
- Downloaded and inspected artifact #13316525198. It contains Room v7 schema JSON with identity hash `3d3bfeb59dfda625d9f6664d1c39765b`, eight entities, and the authenticated-device unique index. The artifact path is `com.familymesh.core.storage.family.FamilyMeshDatabase/7.json`.
- The schema snapshot is current v7 metadata, not a historical migration fixture. No migration from versions 1–6 is claimed as validated.
- Next: finish checking run #173 instrumentation; preserve this v7 schema as a reviewed baseline if appropriate; derive real historical v1–v6 fixtures from reproducible historical builds before adding migration assertions.


## 2026-11-04 — API 34 Room suite green; transient emulator boot failure retried

### Verified work

- Run #41818762325 (#173), source commit `da5e86a94937fcbbc9d0a221b8a2d0c091a50358`, completed both `verify` and `storage-instrumentation` successfully.
- Downloaded instrumentation artifact #13317910182 and inspected its JUnit XML: `tests=6`, `failures=0`, `errors=0`, `skipped=0`; all six DAO tests executed on API 34. XML test-suite time: 72.646 seconds.
- The run also uploaded the current Room v7 schema snapshot and debug APK.
- The historical export workflow `.github/workflows/room-historical-schemas.yml` is present on main, with one pinned historical source commit per Room version 1–7. It remains a manual `workflow_dispatch` workflow and has not yet been run.

### CI failure triage and action

- Run #41819471027 (#175), commit `b0879b22cf97295109a35c5388c43feb0105a346`, passed the complete `verify` job, including unit tests, APK assembly, KSP schema generation, and schema artifact upload.
- Its instrumentation job failed before Gradle started: the emulator did not reach `sys.boot_completed` within the configured 600-second startup window. No tests ran and no JUnit report was produced; this is an emulator startup failure, not a DAO assertion failure.
- Re-ran only the failed instrumentation job through GitHub Actions. The retry was in progress at the time of this update. Do not mark run #175 green until the retry completes successfully.

### Remaining work

1. Execute the historical schema export workflow for Room v1–v7 and inspect the actual JSON outputs before checking fixtures into the repository.
2. Add `MigrationTestHelper` coverage for every supported migration path using those generated historical schemas.
3. Add deterministic SQLite concurrency coverage for the already documented applicant same-artifact/local-receipt-time race; do not alter behavior until its semantics are approved.
4. Keep owner-ledger and exact FMJA1 replay schema/runtime changes blocked on their existing ADR approvals.

No runtime, Room entity, schema version, protocol, cryptography, expiry-policy, or transport behavior changed.


## 2026-11-04 — run #176 full CI and Room instrumentation verified

- Refreshed run [#41828148184](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41828148184) on commit `f2b2988b0a4ffd83784cd3f87b2f2c0fd2a0e082`: both `verify` and `storage-instrumentation` completed successfully.
- Downloaded instrumentation artifact #13325065058 and parsed `TEST-emulator-5554 - 14-_core_storage-.xml`.
- JUnit result: suite `EnrollmentNonceDaoInstrumentedTest`, API 34 emulator, `tests=6`, `failures=0`, `errors=0`, `skipped=0`, suite time 129.86 seconds.
- Confirmed the six executed cases: SQLite uniqueness/family cascade; atomic nonce replay rejection; idempotent owner finalization/conflicting replay; nonce rollback on pending-insert failure; file-backed database reopen durability; and atomic applicant decision persistence.
- This independently closes the immediate emulator retry question: run #175's startup timeout was transient, while run #176 executed the complete suite successfully.
- Migration correctness is still **not** verified: the historical v1–v7 export workflow remains manual and no historical JSON fixtures have been inspected or committed.
- No production runtime, Room entity/schema version, protocol, cryptography, expiry policy, or transport behavior changed.


## 2026-11-04 — Historical Room schema artifacts downloaded and audited

### Work completed

- Downloaded all seven Room schema artifacts from workflow run #41864521401 (artifact IDs 13341926797, 13341822088, 13341247536, 13341317491, 13341438385, 13340463912, and 13341692379).
- Parsed each JSON document and verified its declared version (1–7), identity hash, and entity count. Inspected all entity columns, SQLite affinities, nullability, primary keys, indices, and foreign keys.
- Confirmed entity progression: family spaces/members; nonce table; pending enrollment table; initial owner trust; trusted initial owner; applicant decision; authenticated device.
- Cross-checked exported metadata against current migration SQL and entity definitions. The v5 trusted_initial_owners table intentionally has no foreign key, consistent with the historical entity.
- Added `docs/architecture/room-historical-schema-audit.md` documenting artifact provenance, hashes, schema structure, source consistency, and validation limits.

### Verification and limitations

- All seven ZIPs downloaded and opened successfully; all seven JSON files parsed successfully and declared the expected version.
- This validates authentic exported schema snapshots and source-level shape consistency only. No SQLite migration test, MigrationTestHelper run, or row-preservation assertion has yet been executed.
- Room remains version 7. No runtime, database schema, protocol, crypto, expiry, or transport behavior changed.
- No local Gradle/Android tests were run in this session.

### Next exact action

Refresh run #41864521401 job conclusions. Check the seven authentic JSON files into `core/storage/schemas/com.familymesh.core.storage.family.FamilyMeshDatabase/`, add MigrationTestHelper coverage for all supported start versions 1–7 with representative data and post-migration constraints, and run the tests on API 34.


## 2026-11-08 — Historical Room migration fixture CI follow-up

### Evidence and diagnosis

- Refreshed [Android CI run #41879166661](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41879166661) for main HEAD `968b580283b2ce17d0deda83beab4675ec55b845`.
- The `verify` job passed unit tests, debug APK assembly, and KSP generation/upload of the current Room v7 schema.
- The API 34 instrumentation job executed 7 tests; six passed. The migration test failed at fixture loading with `FileNotFoundException` for `com.familymesh.core.storage.family.FamilyMeshDatabase/1.json`. The test did not reach its migration or preservation assertions.
- Rechecked workflow [#41864521401](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41864521401): all seven historical schema export jobs completed successfully. Parsed v1–v7 JSON and checked version numbers/entity counts (2–8). The v7 file's SHA-256 matches the current main-generated v7 snapshot.

### Work completed in this branch

- Added the seven historical JSON fixtures under `core/storage/schemas/com.familymesh.core.storage.family.FamilyMeshDatabase/`, the directory already registered as androidTest assets.
- Updated the audit, roadmap, project status, and handover to distinguish test-source coverage from an actually passing migration run.
- No production code, database schema version, protocol, cryptographic behavior, expiry policy, or transport behavior changed.

### Verification status

- Fixture JSON validation: passed locally for all 7 versions.
- Current-main unit tests and debug APK build: passed in CI run #184.
- Fixture-backed migration instrumentation: pending branch CI; no migration pass is claimed yet.
- No local Gradle/Android build was run in this session.

### Next exact action

Wait for the branch's API 34 CI result, inspect its JUnit XML, and update this log only with the actual test counts. If migration coverage passes, move on to the outstanding deterministic DAO concurrency/idempotency case without changing behavior before its semantics are reviewed.


## 2026-11-13 — Historical Room migration validation passed; DAO concurrency characterization added

### Verified CI result

- Refreshed [Android CI run #43231837501](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/43231837501) for PR #2 head `2897397fa2e9f672f891c2f2f61f26ada901a372`.
- Both `verify` and `storage-instrumentation` completed successfully.
- Downloaded artifact #13952322275 and parsed `TEST-emulator-5554 - 14-_core_storage-.xml`: 7 test cases, 0 failures, 0 errors, 0 skipped.
- The six DAO cases and the migration test all passed. The migration test iterates starting versions 1–7, seeds representative rows, migrates to v7, verifies row/table preservation, checks all expected indices and the unique family/device index, and exercises FK cascade behavior. The historical v5 `trusted_initial_owners` row is intentionally retained because its v5 schema has no FK.
- The same run passed JVM unit tests, Debug APK assembly, KSP Room v7 schema generation, and artifact uploads.
- Gradle 8.13 wrapper and distribution checksums are pinned; the wrapper-based CI path is now verified.
- No local Gradle/Android build was run in this automation environment.

### Work added after that green run

- Added two deterministic Room DAO concurrency instrumentation cases: same signed decision with different local receipt times, and concurrent opposite decision artifacts.
- The same-artifact test is explicitly a characterization of the current `RECORDED` + `CONFLICT` behavior. It does not approve that behavior as product policy and makes no production code change.
- Updated ADR 0014, the historical-schema audit, roadmap, project status, and handover to reflect the migration pass and the next validation gate.

### Next exact action

Run the updated API 34 instrumentation suite and inspect its JUnit XML. Keep applicant-side receipt-time idempotency semantics open until separately reviewed; keep the owner-issued ledger and exact FMJA1 replay schema changes gated on ADR 0006/0008/0012 approval.


## 2026-11-18 — PR #2 integration candidate validation refresh

- The default branch remains unchanged at `968b580283b2ce17d0deda83beab4675ec55b845`; this work is still isolated on `build/gradle-wrapper-8.13-20261109` and has not been merged.
- PR #2 candidate head: `dc16231b10276443b5169876c4af2ff1232c824e`.
- Android CI run [#43602180283](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/43602180283), attempt 4, completed successfully on 2026-11-17. All three jobs passed: `verify`, API 34 `storage-instrumentation`, and API 31 `identity-api31-instrumentation`.
- The verify job passed JVM unit tests, Debug APK assembly, Room schema generation, and artifact uploads. API 34 instrumentation passed the Room v1–v7 migration suite, DAO concurrency characterizations, and Keystore tests; API 31 Keystore instrumentation also passed.
- Room remains schema v7. The historical migration result validates supported v1–v7 paths only; it is not evidence for a future v7→v8 migration.
- The same-artifact/different-receipt-time and opposite-decision DAO tests are characterization tests. They do not approve idempotency, conflict, expiry, or owner-decision product semantics.
- PR #1 remains superseded for integration by PR #2 and must not be merged independently.
- No production runtime, Room entity/schema version, protocol, cryptographic behavior, expiry policy, or network transport changed in this validation stage.

### Next work and safety gates

Continue the identity/trusted-enrollment review without enabling messaging transport or production authorization. The unresolved gates remain: independent COSE/CBOR vectors for all four artifacts; deterministic encoding and ECDSA low-S policy; v1/v2 key-ID and serialization migration; expiry and replay-retention semantics; trusted owner ceremony, ownership transfer, revocation, recovery; and a threat model covering clock rollback, local database tampering, backup/restore, and offline peers. Do not implement the owner decision ledger or a v7→v8 migration until ADR 0006/0008/0012 semantics are reviewed and approved.


## 2026-11-21 — Consolidate Compose Flow stability and COSE primitive verification

### Work completed

- Ported the reviewed Flow memoization change from PR #4 into the existing PR #2 integration branch. The family-space, pending-enrollment, initial-owner, and trusted-owner Flows are remembered by repository/service identity; the authenticated-device Flow is also keyed by the selected family ID.
- Added the test-only `tools/verify_cose_v2_kat.py` verifier from PR #3 and integrated it into the existing wrapper-based Android CI `verify` job.
- The verifier reconstructs the documented primitive COSE_Key, key ID, payload, external AAD, protected header, Sig_structure, digest, and tagged COSE_Sign1; it checks P-256 scalar bounds, OpenSSL signature verification, and rejection after AAD/payload tampering.
- Updated the roadmap, primitive vector record, project status, and handover to preserve the exact validation boundary.

### Validation evidence and limits

- Previous PR #2 head `49fd3c2100aa0d4210a46ab84e205747b837a019` passed `verify`, API 34 storage instrumentation, and API 31 identity instrumentation in [run #44317110977](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/44317110977).
- PR #3 run [#44743902233](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/44743902233) and PR #4 run [#44866983970](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/44866983970) both passed their `verify` jobs. Their API 34 instrumentation jobs failed before tests ran: the hosted runner disabled hardware acceleration, the emulator reported that `-accel off` was unsupported, and ADB could not find the emulator device. These runs provide no instrumentation result.
- The verifier passed the PR #3 `verify` job and its recorded local Python/OpenSSL checks; the newly integrated PR #2 head still needs its own CI run.
- No local Gradle/Android build was run in this automation environment. No production protocol, cryptographic runtime, Room schema, authorization, or transport behavior changed.

### Next action

Wait for CI on the updated PR #2 head and inspect all three jobs. Keep the COSE profile, owner-decision ledger, expiry semantics, v7-to-v8 migration, authorization, E2EE, and network transport behind their existing review gates.


## 2026-11-22 — Make home-screen Flow collection lifecycle-aware

### Work completed

- Replaced all six `collectAsState` subscriptions in `FamilyMeshHomeScreen` (profile, family spaces, pending enrollment, initial-owner trust, trusted owners, and authenticated devices) with `collectAsStateWithLifecycle`.
- Reused the existing `androidx.lifecycle:lifecycle-runtime-compose` dependency; no new library or version was added.
- Kept the Flow memoization keys unchanged, including the selected family-space ID for authenticated devices. The selected family still switches to its own observation Flow.

### Rationale and validation boundary

- Collection now follows the host Lifecycle and stops below the configured active state, avoiding unnecessary UI-layer subscriptions while the screen is stopped.
- This is a lifecycle/resource-usage improvement, not a measured CPU or battery benchmark. No numeric gain is claimed.
- No repository, Room, domain, protocol, cryptographic, authorization, or transport behavior changed.
- The PR #2 verify job must compile and run unit tests on this head; emulator instrumentation is tracked separately because the latest hosted runner attempt timed out before the emulator became available.


## 2026-11-23 — Preserve coroutine cancellation in Home screen actions

### Findings

- Audited the current PR #2 integration candidate rather than the stale `main` branch.
- `FamilyMeshHomeScreen` contains suspend operations in lifecycle effects and user actions. Several broad `catch (Exception)` handlers and four `runCatching` calls could convert `CancellationException` into an ordinary UI/storage failure.
- The affected suspend operations include local profile updates, family-space creation, device-key security-level reads, owner bootstrap, identity creation, enrollment proof creation/import, decision issuance/import, acceptance/finalization, and owner-key pinning.

### Changes

- Added a small `runSuspendCatching` helper that returns ordinary exceptions as `Result.failure` but always rethrows coroutine cancellation.
- Replaced all four `runCatching` wrappers around suspend work in the Home screen with the cancellation-aware helper.
- Updated broad exception handlers in the Home screen to rethrow `CancellationException` before presenting a user-facing failure.
- Added three JVM tests for cancellation propagation, ordinary exception capture, and success-value preservation.
- No domain, Room schema, authorization, protocol, cryptographic, or transport behavior was changed.

### Validation boundary

- Source transformation checks confirmed that no `runCatching` remains in `FamilyMeshHomeScreen` and all twelve generic exception handlers now rethrow cancellation first.
- The prior PR #2 run [#44989187384](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/44989187384) passed the `verify` job (COSE primitive KAT, JVM unit tests, Debug APK assembly, Room schema generation, and artifact uploads).
- In that same run, API 34 and API 31 instrumentation jobs failed before Gradle tests started: ADB repeatedly reported `emulator-5554 not found`, followed by `Timeout waiting for emulator to boot`. This is an emulator infrastructure failure, not a test assertion result.
- This automation environment did not run a local Gradle build or device profile. The new commit requires its own CI run; no result is claimed yet.
- No numeric CPU, memory, battery, or latency improvement is claimed.

### Next action

Verify the new PR #2 head with CI, including the three new cancellation-helper unit tests. Retry emulator instrumentation only when the runner can provision a working ADB device. Continue protocol review separately; do not introduce the owner-issued ledger or Room v7→v8 migration before ADR 0006/0008/0012 semantics are approved.


## 2026-11-23 — Hosted Linux emulator startup root-cause fix

### Investigation

- Inspected PR #2 head `d7ebfd44ae9d5bf81b5ed02e164764947759fb6a` and Android CI run [#45114550680](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45114550680).
- The `verify` job passed the COSE primitive fixture verifier, JVM unit tests, Debug APK assembly, Room schema generation, and both artifact uploads.
- Both API 31 and API 34 instrumentation jobs failed before Gradle instrumentation started. Raw job logs explicitly reported: `Software CPU emulation (-accel off) is not supported. A hardware CPU accelerator is required (or set ANDROID_I_WANT_MY_TCG=yes to force TCG software emulation).` The runner also reported that hardware acceleration was unavailable.
- This is an emulator-host capability/configuration failure, not a test assertion failure. No instrumentation result is claimed for run #45114550680.

### Change

- Set job-level `ANDROID_I_WANT_MY_TCG: "yes"` in both hosted-Linux emulator jobs so QEMU TCG software CPU emulation is explicitly permitted when KVM/hardware acceleration is unavailable.
- Kept the API levels, emulator image, test commands, timeout, and test coverage unchanged.
- Workflow fix commit: `78f68e1dff252d9e9865d6a25606252e7a9c19e7`.

### Validation boundary

- The workflow edit is committed to PR #2's existing integration branch. A new CI run must confirm whether TCG can boot these images and whether instrumentation tests execute. Until that run completes, emulator validation remains pending.
- No Kotlin runtime, database schema, protocol, cryptography, authorization, or transport behavior changed. No performance gain is claimed.


## 2026-11-23 — Remove duplicate cancellation handlers in Home screen

### Audit finding

- Re-read the complete 921-line `FamilyMeshHomeScreen.kt` on the active integration branch after the prior cancellation-safety change.
- Found four consecutive duplicate `catch (cancelled: CancellationException) { throw cancelled }` clauses in owner approval, decision import, applicant acceptance signing, and owner finalization actions.
- These duplicate clauses added no behavior and obscured the cancellation/error boundary.

### Change

- Removed exactly four redundant cancellation handlers while retaining one cancellation rethrow before each ordinary `Exception` handler.
- Confirmed by source-level audit that no duplicate adjacent cancellation-handler pairs remain and that no `runCatching` calls remain in this screen.
- Commit: `595b189e5707f26666d14fa6886e9f0b010be2ed`.

### Validation boundary

- No local Android build is available in this session. The updated PR CI must compile and test this cleanup.
- This is a behavior-preserving cleanup; no performance gain is claimed. No domain, Room schema, protocol, cryptographic, authorization, or transport behavior changed.


## 2026-11-23 — Increase API 31 emulator boot window

### Evidence

- Inspected PR #2 head `8c15b5df1bb714136a90d8ab8204ef858bcb01fa` and Android CI run [#45166958142](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45166958142).
- The `verify` job passed the primitive COSE fixture verifier, JVM unit tests, Debug APK assembly, Room schema generation, and artifact uploads. API 34 Room/Keystore instrumentation also passed.
- In the API 31 job, the emulator's `sys.boot_completed` value became `1` at approximately 611 seconds. The configured 600-second boot timeout had already elapsed; the action then terminated the emulator and ADB returned exit 224 / broken pipe.
- The Gradle `:core:identity:connectedDebugAndroidTest` command did not start and no API 31 JUnit report was produced. This is an emulator startup timeout, not a failing instrumentation assertion.

### Change

- Increased only the API 31 emulator `emulator-boot-timeout` from 600 to 900 seconds. API 34 retains its 600-second timeout because its job passed in the same run.
- Kept the existing API level, image, test command, and 35-minute job timeout unchanged.
- No Kotlin runtime, identity, database, protocol, cryptography, authorization, or transport behavior changed.

### Validation boundary

- A new PR #2 CI run is required to verify the API 31 test command now executes. No API 31 pass is claimed yet.
- No local Android SDK/Gradle build or physical-device test was available in this automation environment.


## 2026-11-24 — Emulator CI startup reliability and repeat-run cost

### Latest repository state and diagnosis

- Re-synchronized against PR #2 head `90de4eaaa6b7505dd3c06e0c43c8d755c9437803`; `main` remains at `968b580283b2ce17d0deda83beab4675ec55b845`.
- Android CI run [#45191763315](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45191763315) completed with `verify` passing: the COSE primitive verifier, JVM unit tests, Debug APK assembly, Room schema generation, and artifact uploads all succeeded.
- API 34 instrumentation did not start: the emulator never reported `sys.boot_completed=1` before the configured 600-second boot window. No Room/Keystore assertions ran and no JUnit result exists for this job.
- API 31 reached `sys.boot_completed=1`, but the action then failed while sending its unlock key event: ADB returned `Broken pipe (32)` / exit code 224. The Gradle instrumentation command did not start. This is an emulator/ADB startup failure, not a Keystore assertion failure.
- The earlier TCG opt-in removed the explicit unsupported-acceleration error, but did not make cold boots reliable. The workflow therefore must not report either API's latest instrumentation result as passing.

### Change

- Use the AOSP `default` system image instead of `google_apis`; the Room and Android Keystore suites do not use Google Play services.
- Switch to the action's recommended `swiftshader_indirect` software-rendering mode.
- Start the ADB server explicitly before launching each emulator.
- Add separate API 31/API 34 AVD caches and create a clean Quick Boot snapshot on a cache miss; subsequent CI attempts can restore the prepared emulator instead of paying the full cold-boot cost every time.
- Raise the first-boot window to 1,200 seconds and job timeout to 45 minutes so the TCG fallback has bounded startup headroom.
- Keep the same test commands and API coverage. No application runtime, persistence schema, protocol, cryptographic, authorization, or transport behavior changes.

### Validation boundary

- Workflow and status-document changes are committed together to the existing PR #2 branch.
- The next PR CI run is the required validation of the new AOSP image, ADB pre-start, and snapshot-cache path. Until that run finishes, no new API 31/API 34 instrumentation pass is claimed.
- No local Android SDK/emulator build or physical-device profile was available in this run; no performance or battery improvement is claimed.


## 2026-11-24 — Validate imported P-256 public points

### Source audit

- The shared `P256PublicKeyParser` restricted imported SubjectPublicKeyInfo keys to P-256 domain parameters, but did not explicitly check that affine coordinates were in the field or that the point satisfied the curve equation.
- All current signed enrollment artifact verifiers reuse this parser, so consistent point validation belongs at this common trust boundary.

### Change

- Reject the point at infinity, negative/out-of-field coordinates, and points that do not satisfy the P-256 curve equation before returning an imported public key.
- Add JVM tests for an off-curve point and a coordinate equal to the field prime; retain the existing positive P-256 and negative P-384 tests.
- This hardens the current v1 SPKI import/verification path only. It does not implement a v2 COSE key parser, freeze any v2 schema, or satisfy the independent COSE interoperability gate.

### Validation boundary

- This change is staged on `staging/security-and-ci-hardening-20261124` from PR #2 head `93adcffc67d75d70d8b9397b1cd9f7308b169160`; it has not yet been added to PR #2 because run [#45331772915](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45331772915) is validating the preceding emulator-workflow change.
- The JVM tests and Android build must run on the next PR #2 CI head before this parser hardening is treated as verified.
- No benchmark or runtime performance gain is claimed; no Room, protocol, authorization, transport, or UI behavior changed.


## 2026-11-24 — Emulator snapshot diagnosis and single-boot cache redesign

### Run #45331772915 evidence

- The `verify` job passed: draft COSE primitive verifier, JVM unit tests, Debug APK assembly, Room schema export, and artifact uploads.
- API 31 instrumentation completed successfully. Its JUnit XML artifact contains 8 tests, 0 failures, 0 errors, 0 skipped; suite time 45.047 seconds. The job completed successfully.
- API 31 emulator preparation took 419 seconds. The subsequent test action logged `Error -22 while loading VM state` and `Failed to load snapshot 'default_boot'`, then fell back to a cold boot; the emulator became ready at 05:24:09Z. This confirms the two-action snapshot strategy pays for a redundant boot and does not reliably restore the TCG snapshot.
- API 34 emulator preparation completed in 478 seconds; its instrumentation task was still running at the time this staged workflow revision was prepared.
- The current v7 Room schema artifact was downloaded and inspected: it contains schema JSON for versions 1–7; v7 identity hash `3d3bfeb59dfda625d9f6664d1c39765b`, eight entities, the unique `authenticated_family_devices(familySpaceId, deviceKeyId)` index, and five foreign-key declarations.
- The staged P-256 parser and its exact four test methods were compiled/executed locally with Kotlin/JVM on JDK 21 using a minimal JUnit-compatible assertion stub: valid P-256 accepted; P-384, off-curve point, and out-of-field coordinate rejected. This is a focused local check, not the repository Gradle/JDK 17 test task.

### Change

- Remove the separate emulator warm-up action and unreliable Quick Boot snapshot load/save path.
- Restore/cache the AVD definition and corresponding AOSP system-image package, then run the instrumentation command in one emulator action per job.
- Disable snapshots explicitly (`-no-snapshot`) to avoid the observed Error -22 restore path; disable the unused back camera; retain TCG, ADB pre-start, `swiftshader_indirect`, and the 1,200-second boot bound.
- Save the AVD/system-image cache after a successful instrumentation action, so the first successful run seeds subsequent runs without an extra emulator boot.
- Upgrade both repository workflows to Node 24-native action releases: checkout v6, setup-java v6, upload-artifact v6, cache restore/save v5, and setup-gradle v5.

### Validation boundary

- The current PR #2 run [#45331772915](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45331772915) validates the previous two-action workflow, not this single-boot redesign or the staged P-256 code.
- The staged code/workflow changes must be fast-forwarded to PR #2 only after the current run completes, then validated by a fresh PR CI run. No startup speedup or cache-hit benefit is claimed yet.


## 2026-11-24 — Verify complete API 31/API 34 integration run

### Run evidence

- PR #2 candidate head: `93adcffc67d75d70d8b9397b1cd9f7308b169160`.
- Android CI run [#45331772915](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45331772915) concluded `success`; all three jobs completed successfully.
- `verify` passed the Python/OpenSSL primitive KAT check, JVM unit tests, Debug APK assembly, Room schema export, and both artifacts.
- API 31 identity instrumentation: 8 tests, 0 failures, 0 errors, 0 skipped.
- API 34 storage instrumentation: 9 tests, 0 failures, 0 errors, 0 skipped; API 34 identity instrumentation: 8 tests, 0 failures, 0 errors, 0 skipped.
- The API 31 job's earlier emulator snapshot restore logged Error -22 and fell back to cold boot; the Gradle instrumentation command nevertheless ran and passed. This is a workflow-efficiency/reliability issue, not a test failure.

### Candidate follow-up

- `staging/security-and-ci-hardening-20261124` is exactly two commits ahead of the tested PR #2 head. It adds common-boundary P-256 point validation (reject infinity, out-of-field coordinates, and off-curve points) with two JVM negative tests, and replaces the redundant emulator warm-up/snapshot path with a single-boot AVD + system-image cache.
- The staged parser tests received a focused Kotlin/JVM check under JDK 21 with a minimal JUnit-compatible stub, but this is not the repository Gradle/JDK 17 verification.
- The new workflow and parser changes have not yet been exercised by GitHub Actions. They must be fast-forwarded to PR #2 only as a review candidate and validated by a fresh complete CI run.
- No Room schema, protocol wire format, authorization, messaging, or transport behavior changed in these two staged commits.


## 2026-11-24 — PR #2 head 7930484: complete API 31/API 34 CI validation

### Verified evidence

Android CI [run #45406912355](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45406912355) completed with all three jobs successful on PR #2 head `79304843f42ad1ef1fc0e331c2f562a2da731302`.

- `verify`: `./gradlew testDebugUnitTest` completed successfully; `./gradlew assembleDebug` completed successfully; Room schema generation and artifact uploads completed successfully. The job also ran the draft COSE v2 primitive KAT verifier.
- API 34 job: `:core:storage:connectedDebugAndroidTest` completed 9/9 tests with 0 skipped and 0 failed; `:core:identity:connectedDebugAndroidTest` completed 8/8 with 0 skipped and 0 failed.
- API 31 job: `:core:identity:connectedDebugAndroidTest` completed 8/8 with 0 skipped and 0 failed.
- The logs show transient ADB exit-code-1 polling during emulator startup and emulator-console warnings, followed by successful execution of both Gradle instrumentation commands and `BUILD SUCCESSFUL`.
- Uploaded artifacts: Debug APK, Room schema snapshot, API 34 instrumentation reports, and API 31 instrumentation reports.

### Changes covered by this validation

- Explicit P-256 public-point validation at the existing v1 X.509 SPKI import boundary, including point-at-infinity, coordinate range, and curve-equation checks, with malformed-point JVM coverage.
- Single-boot AOSP AVD/system-image cache workflow for API 31 and API 34; no separate warm-up/snapshot restore path.
- Node 24-compatible major versions of the GitHub Actions used by the workflow.

### Boundaries and next work

- This validates the tested PR head on GitHub-hosted emulator images. It does not prove hardware-backed Keystore protection, remote attestation, physical-device behavior, or performance/battery improvements.
- PR #2 remains Draft and unmerged; main remains unchanged. Room remains v7.
- The COSE v2 proposal is still not approved. Complete four-artifact schemas/vectors, two independent COSE implementations, DER-to-R||S and low-S policy, key-ID migration, time/expiry/replay semantics, owner-issued decision authority, exact FMJA1 replay identity, and owner trust/revocation/recovery remain open.
- Do not implement v7→v8 or enable production authorization/network transport until the relevant ADRs and security review are approved.


## 2026-11-25 — Add Android Lint to the integration verification gate

### Fresh repository synchronization

- Default branch `main` remains at `968b580283b2ce17d0deda83beab4675ec55b845`; no candidate has been merged.
- PR #2 remains open, Draft, and unmerged. Its actual head before this change was `785733affcab42ec640819ce154e51099d2ecfaa`; the preceding source/integration head was `79304843f42ad1ef1fc0e331c2f562a2da731302`.
- Android CI run [#45406912355](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45406912355) passed all three jobs on source head `79304843f42ad1ef1fc0e331c2f562a2da731302`: JVM verification/KAT/Debug APK/Room schema export, API 31 identity 8/8, API 34 storage 9/9, and API 34 identity 8/8; all instrumentation suites had zero failures and zero skipped tests.
- Follow-up run [#45412603517](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45412603517) passed all three jobs on `785733affcab42ec640819ce154e51099d2ecfaa`; that commit changed the roadmap's CI reference only.
- No local Android SDK/Gradle execution or physical-device performance profile was available in this run.

### Change

- Add `./gradlew lintDebug` to the `verify` job after JVM unit tests. This asks the Android Gradle Plugin's Android Lint task to analyze the Debug variant in the multi-module Android build before APK packaging.
- This is a CI quality gate only; no application runtime, cryptography, protocol, persistence schema, authorization, or transport behavior changes.

### Validation result

- Android CI [run #45552834794](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45552834794) completed successfully on PR #2 head `a5a86de07ded532685249bf734111915cde20646`; all three jobs passed.
- The verify job completed the COSE primitive KAT, `testDebugUnitTest` (39 seconds), aggregate `lintDebug` (37 seconds), `assembleDebug` (14 seconds), Room schema generation (7 seconds), and both existing artifacts.
- The `lintDebug` task executed for `:app`, `:core:common`, `:core:designsystem`, `:core:identity`, and `:core:storage`. It exited successfully. The detailed HTML/XML reports were not uploaded in this run, so no issue-count claim is made.
- API 31 Keystore instrumentation: 8 tests, 0 failures, 0 errors, 0 skipped; JUnit suite time 33.024 seconds.
- API 34 storage instrumentation: 9 tests, 0 failures, 0 errors, 0 skipped; JUnit suite time 109.933 seconds. API 34 identity instrumentation: 8 tests, 0 failures, 0 errors, 0 skipped; JUnit suite time 28.071 seconds.
- No physical-device, hardware-backed attestation, or performance/battery profile was run. No numeric performance gain is claimed.


## 2026-11-25 — Stage Android-provider P-256 parser instrumentation

### Audit rationale

- The shared P-256 parser now performs explicit field-bound and curve-equation checks, and its JVM tests pass in the integrated PR CI.
- The verifier is also used on Android devices, where the JCA provider and Android Keystore-generated X.509 public-key encoding are platform-specific. Existing Keystore instrumentation verified signatures through JCA, but did not directly exercise the shared parser.

### Staged tests

- Add an Android instrumentation test that feeds an Android Keystore-generated P-256 SPKI through `P256PublicKeyParser`.
- Add Android-provider negative cases for an off-curve point and a coordinate equal to the P-256 field prime.
- Keep these as test-only changes; no production parser, key format, protocol, persistence, or authorization behavior changes.

### Validation boundary

- Staged on `staging/android-p256-provider-coverage-20261125`, based on `a5a86de07ded532685249bf734111915cde20646`; not yet integrated into PR #2.
- A local Kotlin/JVM check with JDK 21 compiled the fixture helper and confirmed that a generated P-256 SPKI is 91 bytes and both malformed-point fixtures preserve the DER length. This checks the test helper only; it is not an Android-provider or repository Gradle test.
- The existing PR run [#45552834794](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45552834794) completed successfully, but it predates these three new instrumentation tests. They still require API 31 and API 34 execution in a fresh PR CI run.


## 2026-11-25 — Preserve Android Lint reports as CI artifacts

- Extend the staged Android CI change to upload all module-level Debug Lint HTML and XML reports, including when an earlier verification step fails.
- This makes analyzer findings inspectable after the hosted runner is gone and supports evidence-based cleanup rather than relying only on the Gradle exit code.
- The upload is staged on `staging/android-p256-provider-coverage-20261125` after the Android-provider parser instrumentation tests.
- Validation is pending a fresh PR CI run; no report contents or issue count are claimed yet.


## 2026-11-25 — Full CI validation of Android Lint integration candidate

- PR #2 candidate head `a5a86de07ded532685249bf734111915cde20646` passed [Android CI run #45552834794](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45552834794); `verify`, API 31 identity, and API 34 storage/identity jobs all completed successfully.
- Aggregate Android Lint ran across all five Android modules and passed. The next candidate additionally uploads the HTML/XML reports so findings can be inspected; this artifact-upload change has not yet been exercised.
- API 31 identity: 8/8; API 34 storage: 9/9; API 34 identity: 8/8. All suites had zero failures, errors, or skipped tests.
- No runtime behavior, Room schema, protocol, cryptography, authorization, or transport changed in this validation cycle.


## 2026-11-25 — Triage Android Lint security and launcher findings

### Report findings

- The uploaded Android Lint artifact from [run #45554437797](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45554437797) contains 19 app warnings and zero warnings in each of the four core modules.
- Two findings are directly actionable without changing protocol or platform compatibility: missing launcher icon and missing Android 12+ data-extraction rules.
- Keep `android:allowBackup="false"` and explicitly exclude app data from both cloud backup and device-to-device transfer. Android's official guidance notes that `allowBackup=false` may not disable device-to-device transfer on some Android 12+ manufacturers, so the transfer rules must be explicit.
- Add a simple adaptive FamilyMesh network-node launcher icon, including a monochrome layer for themed-icon support.

### Deferred warnings

- Keep `targetSdk = 36`; Lint's OldTargetApi warning is not a reason to jump to an unreleased or untested target. Revisit only with the next stable platform and behavior-change testing.
- Do not blindly upgrade AGP/Gradle, Kotlin/KSP, Compose BOM, Lifecycle, Room, DataStore, Koin, or Coroutines solely to silence version-availability warnings. Review release notes, plugin compatibility, Room schema/KSP behavior, and run the full CI matrix before any coordinated dependency update.

### Validation boundary

- Changes are staged on `staging/manifest-backup-and-icon-20261125` from `8a744e44da561e5622272a39f669c5b599535104`; they are not integrated into PR #2 yet.
- The current PR CI run #45554437797 is still validating Android-provider parser tests and Lint report upload. This branch requires its own fresh CI after integration.
- No runtime, encryption, key, database, authorization, or transport behavior is intentionally changed.
