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

- No Gradle Wrapper yet → CI installs Gradle 8.13 + JDK 17
- `android-ci.yml`: unit tests, assembleDebug, APK artifact; storage instrumentation on emulator
- Do not treat a green `verify` job alone as full security or migration proof

## Throughput for hourly automations

Maximize useful work per run: fix CI, close test gaps, advance the **current** work package from HANDOVER, update status docs. Stop only when blocked on product/security decisions listed in ADRs (e.g. expiry policy ADR 0006) or on human review gates.
