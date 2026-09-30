# Release Notes: DueSoon v2.0.0

**Release Date:** September 2026
**Version Code:** 11

## Major Features
### 🌐 Cloud & Multi-Device Synchronization (Opt-In)
Take your tasks anywhere with DueSoon's brand new Cloud Sync architecture! 
* **Seamless Multi-Device Support:** Add, edit, or check off tasks on your phone, and instantly see the updates on your tablet.
* **100% Offline-First:** Sync is completely optional. If you don't enable it, DueSoon behaves exactly as it always has. Your tasks remain securely stored on your device and are never transmitted.
* **End-to-End Encrypted (E2EE):** Privacy is our top priority. If you enable Cloud Sync, your tasks are encrypted on your device using a secure passphrase *before* they are sent to the cloud. Our servers receive only unreadable ciphertext and can never access your data. 

## Technical Details (For Developers & Power Users)
- **Zero-Knowledge Encryption:** We utilize AES-256-GCM combined with a 120k iteration PBKDF2 key derivation function. Your Master Encryption Key never leaves your device and is safely stored in Android's Keystore.
- **Conflict Resolution:** We employ a robust Last-Writer-Wins (LWW) protocol that resolves edits based on UTC timestamps to handle conflicting offline edits gracefully.
- **Database Migration:** Room Schema has been upgraded (v3 -> v4) providing UUID population to support unique decentralized identification.
- **Performance:** Background sync operations run intelligently over WorkManager under optimal battery/network constraints.

## Fixes & Improvements
- Added Pull-to-Refresh functionality on the Home Screen for instantaneous sync triggers.
- Resolved various minor UI alignment and configuration display issues in the Settings screen.
