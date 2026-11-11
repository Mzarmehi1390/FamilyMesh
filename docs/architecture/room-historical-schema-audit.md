# Historical Room Schema Artifact Audit

- Date: 2026-11-04
- Repository: `Mzarmehi1390/FamilyMesh`
- Branch at audit: `main`
- Source HEAD at audit: `cdda0bd46d67bda94051059c9f99cc7d0f5c064c`
- Workflow run: [Historical Room schema export #1](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41864521401)
- Scope: inspect the seven Room JSON schema artifacts produced from pinned historical source commits; no runtime or schema changes.

## Artifact inventory

| Room version | Pinned source commit | Artifact ID | Identity hash | Entity count |
|---:|---|---:|---|---:|
| 1 | `669b4271e5636e14f22d0332a966243d50d5f53f` | 13341926797 | `70dea20b640bfa5347bfacafbd3da238` | 2 |
| 2 | `d7e59df0a3c600e5fb85500ddc795dd1f77ac7fde` | 13341822088 | `7eb54c09bcd8f5f8c9d2462a81e262b8` | 3 |
| 3 | `b1ba56e4743fd8034973061743ab1c776dad4e27` | 13341247536 | `08336630c165958057bda69b6f777cc2` | 4 |
| 4 | `74cc368f474c34b2a9aac5145b79feb046c11506` | 13341317491 | `d10dbf2b2cf47e97ec562729347ff5c4` | 5 |
| 5 | `f39b23dd6dc182ecf4ab6359446406b5ba773465` | 13341438385 | `b9023c98bf2c4afc51ca3aec8995d352` | 6 |
| 6 | `814bf4434362fd82536fd6b625ce75cee4411552` | 13340463912 | `e93f1499fa69b7606878f8350a476d70` | 7 |
| 7 | `64ff34e4612aaf17a1874ad7a4619966e51cb329` | 13341692379 | `3d3bfeb59dfda625d9f6664d1c39765b` | 8 |

Each downloaded ZIP contained exactly one JSON schema at
`com.familymesh.core.storage.family.FamilyMeshDatabase/<version>.json`.
All JSON documents parsed successfully and declared the matching Room
version. The v7 identity hash matches the separately inspected current v7
schema snapshot.

## Entity progression

| Version | Entities |
|---:|---|
| 1 | `family_spaces`, `family_members` |
| 2 | v1 + `consumed_enrollment_nonces` |
| 3 | v2 + `pending_enrollment_requests` |
| 4 | v3 + `initial_owner_trust` |
| 5 | v4 + `trusted_initial_owners` |
| 6 | v5 + `enrollment_decisions` |
| 7 | v6 + `authenticated_family_devices` |

The entity additions align with the documented migration chain:
1→2, 2→3, 3→4, 4→5, 5→6, and 6→7.

## Constraint and migration-source cross-check

The exported JSON metadata was inspected for every entity's columns,
SQLite affinity, nullability, primary key, indices, and foreign keys.

- v1 `family_members.familySpaceId` has an index and a cascade FK to
  `family_spaces(id)`.
- v2 adds `consumed_enrollment_nonces`, with `nonceDigest` as its primary key.
- v3 adds `pending_enrollment_requests`, with composite PK
  (`familySpaceId`, `requestId`), indexes on `receivedAtEpochMillis`
  and `deviceKeyId`, and a cascade FK to `family_spaces(id)`.
- v4 adds `initial_owner_trust`, keyed by `familySpaceId`, indexed by
  `ownerDeviceKeyId`, and cascade-linked to `family_spaces(id)`.
- v5 adds `trusted_initial_owners`, keyed by `familySpaceId` and indexed
  by `ownerDeviceKeyId`. It has no FK in the exported historical schema;
  this is intentional and matches the entity definition.
- v6 adds `enrollment_decisions`, with composite PK, owner-key and
  recording-time indices, and a cascade FK to `family_spaces(id)`.
- v7 adds `authenticated_family_devices`, with composite PK, a unique
  composite index on (`familySpaceId`, `deviceKeyId`), and a cascade FK
  to `family_spaces(id)`.

The exported metadata is consistent with the current source's migration
SQL and entity definitions for the described version transitions. This is
a schema-shape/source cross-check, not execution of the migrations.

## Important limitations

- The workflow uses pinned historical source commits and applies a temporary
  build-time patch to enable Room schema export. It does not rewrite those
  historical Git commits.
- Artifact presence and JSON validity do not by themselves prove that the
  workflow matrix jobs have all reached a successful terminal state; the
  GitHub run/job status should still be refreshed and recorded.
- No `MigrationTestHelper` test has yet opened these historical schemas.
- No row-preservation, SQLite migration, index/FK post-migration, or
  database-upgrade test is claimed as passed.
- Room remains schema version 7. No v8 migration, owner-ledger persistence,
  expiry behavior, protocol, crypto, or transport behavior was changed.

## Current validation state

- The seven authentic schema JSON files are included in branch
  `fix/room-migration-schema-fixtures-20261108` under
  `core/storage/schemas/com.familymesh.core.storage.family.FamilyMeshDatabase/`.
- Each fixture parses and declares its matching version (1–7); entity counts
  progress from 2 through 8. The v7 fixture is byte-identical to the v7
  snapshot generated from the current main commit.
- CI run [#41879166661](https://github.com/Mzarmehi1390/FamilyMesh/actions/runs/41879166661)
  passed the unit-test, APK-assembly, and current-schema-export job. Its
  instrumentation job exposed the missing-fixture packaging issue described
  above. The fixture-backed migration result is still pending.

## Next engineering action

1. Run the branch's API 34 `storage-instrumentation` job and inspect the JUnit
   XML for all seven migration start versions and the DAO tests.
2. If it passes, record the verified migration result and continue with the
   separately documented DAO concurrency/idempotency gap.
3. Keep v7→v8 owner-ledger and replay-identity work gated on ADR
   0006/0008/0012 review; this fixture work does not approve a schema change.
