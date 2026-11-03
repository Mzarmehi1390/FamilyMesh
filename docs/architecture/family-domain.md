# Family domain model

The first domain layer contains immutable representations of a family space and
its member records. Constructors enforce basic shape constraints (non-blank IDs
and names, bounded display-name lengths, and non-negative timestamps).

These records are **not** proof of identity, authorization, or membership.
IDs are supplied by a future enrollment/repository layer. The local device
profile ID must not be treated as a network identity or cryptographic key.

No persistence, invitation flow, remote synchronization, or network behavior is
implemented by this model. Those concerns require explicit enrollment and
security design before implementation.
