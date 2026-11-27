# Agent guide (AI and automation)

Concise rules for automated coding agents working on FamilyMesh.

## Mission

Private, decentralized, **end-to-end encrypted by design** family messaging on **native Android**. Advance Milestone 1 (identity & trusted enrollment) carefully. Do not invent production security claims.

## Read first every session

1. [HANDOVER.md](../HANDOVER.md) — current package, blockers, next exact action
2. [PROJECT_STATUS.md](../PROJECT_STATUS.md) — what is implemented vs blocked
3. [docs/roadmap.md](./roadmap.md) — milestone checklist
4. Relevant ADRs under `docs/architecture/decisions/` before schema/protocol changes

## Hard invariants

- **Native Android only:** Kotlin, Jetpack Compose, Gradle Kotlin DSL, modules `app`, `core:common`, `core:storage`, `core:designsystem`, `core:identity`.
- **No centralized messaging backend** as a substitute for the product model.
- **Never claim** E2EE messaging, P2P, BLE, Wi-Fi Direct, relay, production membership authorization, revocation, or recovery exist until implemented and reviewed.
- **Authorization stays fail-closed** (`DenyAllFamilyMembershipResolver` default) until a reviewed trusted membership resolver exists.
- **Do not silently replace** a lost/invalidated device key.
- Enrollment today is **local / manual text transfer** — not a network protocol.

## Allowed high-value work

- Unit tests, Room DAO / instrumentation tests, migration fixtures (per ADR 0009/0014)
- CI reliability (emulator boot, Gradle Wrapper when ready)
- Documentation accuracy (HANDOVER, PROJECT_STATUS, WORK_LOG, ADRs)
- Security hardening of existing enrollment codecs/verifiers
- Additive, reviewed schema work **only after** documented ADR gates (e.g. 0006, 0008, 0012)

## Disallowed without explicit human approval

- Implementing network transport, BLE, Wi-Fi Direct, or relays
- Claiming COSE v2 / production wire format is “done” without vectors + review
- Weakening fail-closed authorization
- Breaking Room migration chain or deleting enrollment request facts required for FMJA1
- Shipping secrets, debug backdoors, or cleartext traffic enables

## Quality bar

- Prefer evidence from CI (unit + instrumentation) over narrative claims
- Update `WORK_LOG.md` and refresh `HANDOVER.md` / `PROJECT_STATUS.md` when you change status
- Keep commits focused; message should state security/test impact
- Schema changes: additive migrations only; coordinate owner ledger + replay digest per ADR 0012

## Build / CI notes

- `main` / this branch still lack the Gradle Wrapper. PR #2 carries the pinned Gradle 8.13 wrapper candidate and remains Draft/unmerged; its latest head `f47634412bf0f2c9b967a1f55fe6cdf567b79244` is under Android CI run [#45943777424](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45943777424), which was still in progress at the 2026-11-27 synchronization. The earlier green run was on an older head.
- `android-ci.yml`: unit tests, assembleDebug, APK artifact; storage instrumentation on emulator
- Do not treat a green `verify` job alone as full security or migration proof

## Throughput for hourly automations

Maximize useful work per run: fix CI, close test gaps, advance the **current** work package from HANDOVER, update status docs. Stop only when blocked on product/security decisions listed in ADRs (e.g. expiry policy ADR 0006) or on human review gates.

## 2026-11-27 — Live repository and CI synchronization

- Live PR metadata confirms `develop/agent-ready` is the head of documentation PR #5 at `f15fa4410c2f84b083e4563b683d5ba0dc2e4f39`; its base is `main` at `968b580283b2ce17d0deda83beab4675ec55b845`. This branch remains documentation-only relative to main; do not treat source changes from other PRs as present here.
- The integration candidate PR #2 has advanced since the prior handoff: current head is `f47634412bf0f2c9b967a1f55fe6cdf567b79244` on `build/gradle-wrapper-8.13-20261109`, still Draft and unmerged. Android CI run [#45943777424](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45943777424) (run 236) was observed **in progress**: all three jobs had started; COSE KAT and both backup-policy checks passed in `verify`, while unit tests and the three job completions were still pending at synchronization. Do not describe this run as green until its final conclusions are checked.
- PR #5's latest observed Android CI run [#45862106356](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45862106356) (run 223) completed with `verify` passed and `storage-instrumentation` failed before Gradle instrumentation ran: ADB repeatedly reported `device 'emulator-5554' not found`, followed by `Timeout waiting for emulator to boot`. This is an emulator startup failure, not a test assertion failure; it is still a failed required job.
- PR #6 remains open and unmerged. Its CI run [#45882198628](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45882198628) (run 228) passed both `verify` and `storage-instrumentation`. This validates PR #6's exact head `b22912f0fe175cccc5c0c5b44162da876c3ad230`, not other branches.
- The previous successful PR #2 run [#45760437373](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45760437373) was on the older head `8f94a1a3dc26e124a748212077b6e70bdfff44f6`; it is historical evidence only for that exact commit.
- Keep PR #2 Draft pending its current CI and the documented protocol/security/owner reviews. Do not merge PR #3/#4 independently; their work is already represented in the integration candidate. Do not implement Room v8, owner-ledger/FMJA1 digest, or expiry-policy changes before ADR 0006/0008/0012 approval. Membership authorization remains fail-closed; lost device keys must not be replaced.
- No local clone, Gradle build, Android Lint, or local test execution was available through the connected GitHub interface in this run. All validation statements above refer only to the named GitHub Actions runs.
