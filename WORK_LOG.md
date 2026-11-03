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
