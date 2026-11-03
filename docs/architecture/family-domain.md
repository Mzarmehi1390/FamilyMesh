# Family Domain

Family-space and member records are immutable domain values. A local profile ID
must not be treated as a network identity or cryptographic key.

## Repository boundary

`FamilySpaceRepository` exposes observable family-space and member lists,
creates family spaces, and can add a local member-list entry.

## Local Room implementation

The storage module provides a Room-backed implementation for family-space
records and local member entries. Creating a space trims and validates its
name, generates a random local UUID, and persists the record. Adding a member
trims and validates the display name, generates a local UUID, and stores the
least-privileged `MEMBER` role. The database foreign key requires the referenced
family space to exist and cascades member-record deletion when a space is deleted.

Member entries are local metadata only. The operation does not verify a
person's identity, prove device ownership, grant trusted permissions, or
establish membership on another device. Owner/admin assignment is deliberately
not exposed by this local enrollment API.

This is device-local storage only. It does not authenticate a person or device,
create invitations, synchronize with another device, or provide end-to-end
encryption. No network behavior is implemented.
