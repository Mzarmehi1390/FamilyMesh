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
4. PR #2 contains a pinned Gradle 8.13 Wrapper candidate; it has passed its current CI but remains Draft/unmerged, so `main` still does not include it.

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
