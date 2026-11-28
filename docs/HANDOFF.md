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

1. Prefer **green CI** and expanding Room migration / instrumentation coverage (historical fixtures v1–v7 checked in; foreign-key PRAGMA now enabled in migration tests; emulator job hardened against flaky system crashes with a single-command retry).
2. **Do not** implement Room v8 owner-ledger / FMJA1 digest until ADR gates (0006 expiry policy, 0008/0012 design approval) are satisfied — document and test toward those gates if blocked.
3. Protocol review remains open (COSE v2 draft ADR 0003; custom v1 envelopes still ship). Four primitive KATs now cover all artifact contexts (0001–0004), negative-test outline includes concrete mutation examples for all fifteen checklist items (including invalid point, wrong curve, indefinite length, duplicate keys, and prior cases), a review-only payload schema proposal is available, and low-S/signature-conversion review notes have been added. Full schemas remain open.
4. PR #2 contains a pinned Gradle 8.13 Wrapper candidate and remains Draft/unmerged, so `main` still does not include it. Its current head has full green CI.

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

## 2026-11-28 — Expanded negative-test mutations

- Negative-test outline expanded with concrete examples for invalid point, wrong curve, indefinite length, and duplicate keys (completing concrete illustrations for all fifteen checklist items).
- Latest Android CI on this branch remains green.
- PR #2 remains the integration candidate.
- Room remains v7. Keep ADR 0006/0008/0012 implementation gates closed. Authorization remains fail-closed; do not replace lost device keys.
