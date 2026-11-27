# FamilyMesh — AI / automation handoff

**Repository:** https://github.com/Mzarmehi1390/FamilyMesh  
**Default branch:** `main`  
**Agent-oriented docs branch (if unmerged):** `develop/agent-ready`

This file is the short entry point. Detailed history lives in the longer docs.

| Doc | Use |
| --- | --- |
| [HANDOVER.md](../HANDOVER.md) | Authoritative next actions, blockers, CI notes |
| [PROJECT_STATUS.md](../PROJECT_STATUS.md) | Capability matrix and verification evidence |
| [WORK_LOG.md](../WORK_LOG.md) | Chronological engineering log |
| [docs/roadmap.md](./roadmap.md) | Milestone checklist |
| [docs/AGENTS.md](./AGENTS.md) | Rules for AI agents |
| [docs/architecture/overview.md](./architecture/overview.md) | Architecture |
| ADRs in `docs/architecture/decisions/` | Design gates before implementation |

## Product in one paragraph

FamilyMesh is a **native Android** app for private family messaging. Milestone 0 foundation is in place. Milestone 1 has a **local, manually transferred** enrollment prototype (signed request → owner decision → applicant acceptance → owner finalization into Room). It is **not** production messaging: no E2EE messages, no P2P/network, no production authorization resolver.

## Stack

- Android 12+ (API 31), Kotlin, Jetpack Compose, Koin, Room, DataStore
- Modules: `app`, `core:common`, `core:storage`, `core:designsystem`, `core:identity`
- CI: GitHub Actions (`android-ci.yml`, room historical schema workflow)

## Current engineering focus (do this next)

From HANDOVER (verify against latest `main` each run):

1. Prefer **green CI** and expanding Room migration / instrumentation coverage (historical fixtures v1–v7, MigrationTestHelper).
2. **Do not** implement Room v8 owner-ledger / FMJA1 digest until ADR gates (0006 expiry policy, 0008/0012 design approval) are satisfied — document and test toward those gates if blocked.
3. Protocol review remains open (COSE v2 draft ADR 0003; custom v1 envelopes still ship).
4. PR #2 contains a pinned Gradle 8.13 Wrapper candidate and remains Draft/unmerged, so `main` still does not include it. Its current head has full green CI; see the 2026-11-27 confirmation below.

## What NOT to do

- Replace decentralized design with a central chat server
- Claim security properties that are not implemented
- Open authorization to “allow all” for convenience
- Large speculative refactors unrelated to the current work package

## For hourly automation runs

1. Read HANDOVER + PROJECT_STATUS + roadmap + AGENTS
2. Check latest Actions on `main` / this branch
3. Execute as many unblocked checklist items as possible in one sprint
4. Commit via GitHub tools; refresh HANDOVER/PROJECT_STATUS/WORK_LOG when status changes
5. Reply to the owner in **Persian** with commits, CI links, and remaining blockers


## Verified CI refresh — 2026-11-26

- PR #2 candidate head `8f94a1a3dc26e124a748212077b6e70bdfff44f6` passed all three jobs in [Android CI run #45760437373](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45760437373): verify, API 31 identity/Keystore instrumentation, and API 34 storage/Keystore instrumentation.
- PR #2 remains Draft/unmerged. Its source changes are not part of `main` or this documentation branch yet.
- PR #3/#4 instrumentation failures were emulator-startup failures (ADB could not find emulator-5554); their verify jobs passed. Their changes are superseded for integration by PR #2.
- Room remains v7. Keep ADR 0006/0008/0012 implementation gates closed pending explicit approval; authorization remains fail-closed; do not replace lost device keys.

## 2026-11-27 — Live repository and CI synchronization

- Live PR metadata confirms `develop/agent-ready` is the head of documentation PR #5; its base is `main` at `968b580283b2ce17d0deda83beab4675ec55b845`. This branch remains documentation-only relative to main; do not treat source changes from other PRs as present here.
- The integration candidate PR #2 has advanced: current head is `f47634412bf0f2c9b967a1f55fe6cdf567b79244` on `build/gradle-wrapper-8.13-20261109`, still Draft and unmerged.
- Android CI run [#45943777424](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45943777424) (run 236): **all three jobs completed successfully** — `verify` (COSE KAT, backup-policy checks, unit tests, Lint, APK, schema export), API 31 identity/Keystore instrumentation, and API 34 storage/Keystore instrumentation. Instrumentation reports uploaded.
- PR #5's latest Android CI run [#45944769749](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45944769749): `verify` succeeded; `storage-instrumentation` failed with emulator boot timeout (`adb: device 'emulator-5554' not found`). This is infrastructure flakiness, not a test assertion failure. The workflow on this docs branch uses the older emulator setup without AVD cache (unlike PR #2).
- PR #6 remains open and unmerged. Its CI run [#45882198628](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45882198628) (run 228) passed both `verify` and `storage-instrumentation`. This validates PR #6's exact head `b22912f0fe175cccc5c0c5b44162da876c3ad230`, not other branches.
- The previous successful PR #2 run [#45760437373](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45760437373) was on the older head `8f94a1a3dc26e124a748212077b6e70bdfff44f6`; it is historical evidence only for that exact commit.
- Keep PR #2 Draft pending the documented protocol/security/owner reviews. Do not merge PR #3/#4 independently; their work is already represented in the integration candidate. Do not implement Room v8, owner-ledger/FMJA1 digest, or expiry-policy changes before ADR 0006/0008/0012 approval. Membership authorization remains fail-closed; lost device keys must not be replaced.
- No local clone, Gradle build, Android Lint, or local test execution was available through the connected GitHub interface in this run. All validation statements above refer only to the named GitHub Actions runs.

## 2026-11-27 (later) — Incremental security test expansion

- Created feature branch `test/p256-parser-negative-cases` from `main` and pushed a commit expanding `P256PublicKeyParserTest` with empty, truncated, non-EC, and malformed DER rejection cases. This is pure test hardening of the existing fail-closed P-256 boundary; no runtime change.
- PR #2 already contains a more comprehensive set of P-256 negative cases (invalid points, non-canonical DER, infinity, trailing bytes). The simple branch is superseded for integration; no separate PR was opened for it.
- Instrumentation jobs on PR #2 completed successfully; the docs branch instrumentation remains flaky due to emulator startup.
