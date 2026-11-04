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
