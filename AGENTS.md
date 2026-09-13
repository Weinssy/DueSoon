Project: DueSoon

DueSoon is a minimal, local-first Android deadline reminder app.

Technology:
- Kotlin
- Jetpack Compose
- Material 3
- Room
- Coroutines
- Flow
- StateFlow
- Navigation Compose
- DataStore
- Android Notification APIs

Architecture:
UI → ViewModel → Repository → Room

Rules:
1. Do not introduce unnecessary dependencies.
2. Do not add cloud/backend functionality.
3. Do not add authentication.
4. Do not add AI features in MVP.
5. Keep the UI minimal and deadline-focused.
6. Follow Material 3.
7. Keep business logic out of Composables.
8. Use ViewModel for UI state.
9. Use Repository between UI/domain and Room.
10. Build and test after significant changes.
11. Do not modify unrelated files.
12. Preserve existing functionality when implementing new features.