# Family Domain

Family-space and member records are immutable domain values. A local profile ID
must not be treated as a network identity or cryptographic key.

## Repository boundary

`FamilySpaceRepository` exposes observable family-space and member lists and a
suspending create operation.

## Local Room implementation

The storage module now provides a Room-backed implementation for family-space
records. Creating a space trims and validates its name, generates a random
local UUID, and persists the record. Family-space and member lists are exposed
as observable flows. The member table is prepared with a foreign key to its
family space, but no member-enrollment or member-write operation exists yet.

This is device-local storage only. It does not establish family membership,
authenticate a person or device, create invitations, synchronize with another
device, or provide end-to-end encryption. No network behavior is implemented.
