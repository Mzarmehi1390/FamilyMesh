# Local identity boundary

The first local profile is deliberately device-scoped.

- A random UUID is generated once and stored in app-private DataStore.
- The display name is local metadata and is limited to 40 characters.
- No profile field is uploaded or shared with another device.
- This UUID is **not** a cryptographic key, account identifier, invitation token, or proof of identity.
- A future E2EE identity must use dedicated cryptographic key material, Android Keystore-backed protection where appropriate, and a reviewed key lifecycle. It must not reuse this display UUID.

Clearing app data removes this local profile. Multi-device identity and recovery are not implemented.
