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

- `main` / this branch still lack the Gradle Wrapper. PR #2 carries the pinned Gradle 8.13 wrapper candidate and remains Draft/unmerged; its latest head `f47634412bf0f2c9b967a1f55fe6cdf567b79244` has green Android CI ([#45943777424](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/45943777424)).
- `android-ci.yml`: unit tests, assembleDebug, APK artifact; storage instrumentation on emulator
- Do not treat a green `verify` job alone as full security or migration proof

## Throughput for hourly automations

Maximize useful work per run: fix CI, close test gaps, advance the **current** work package from HANDOVER, update status docs. Stop only when blocked on product/security decisions listed in ADRs (e.g. expiry policy ADR 0006) or on human review gates.

## 2026-11-27 — Protocol-review support expanded with KAT 0002

- `develop/agent-ready` (PR #5) now includes COSE v2 primitive KAT 0002 (OwnerDecision context) plus updated negative-test outline and ADR 0003 references.
- Prior green Android CI runs on this branch remain valid for the storage/fixture work. New documentation commits have triggered fresh CI (in progress at time of writing).
- Integration candidate PR #2 remains Draft at head `f47634412bf0f2c9b967a1f55fe6cdf567b79244` with its own green CI.
- Historical Room fixtures and migration CASCADE coverage are validated on this branch.
- Keep PR #2 Draft pending protocol/security/owner reviews. Do not implement Room v8, owner-ledger/FMJA1 digest, or expiry-policy changes before ADR 0006/0008/0012 approval. Membership authorization remains fail-closed; lost device keys must not be replaced.
