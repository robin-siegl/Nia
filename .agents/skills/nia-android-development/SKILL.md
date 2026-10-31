---
name: nia-android-development
description: >-
  Implement and review Android changes in nia using Kotlin, Jetpack Compose, coroutines and DataStore. Use for UI, settings, themes, persistence, backups, tests, dependencies and Gradle or release metadata. Pair with nia-launcher-development for launcher lifecycle, discovery, launching, gestures, unlocks or profile behavior.
---

# Develop Android features in nia

## Establish the change

1. Read [AGENTS.md](../../../AGENTS.md), [architecture](../../../docs/ARCHITECTURE.md) and the affected source/tests. Read [design](../../../docs/DESIGN.md) for visible changes.
2. Identify the state owner, affected boundary and failure case before editing. Reuse the existing package structure and patterns.
3. Check current official Android/AndroidX documentation for changed API, SDK, dependency or permission behavior. Use primary sources; do not upgrade unrelated dependencies as part of a feature.

## Implement within the existing boundaries

- Keep UI composables driven by values and callbacks. Route persistence and platform operations through the existing ViewModel/data/platform boundaries.
- Collect flows with lifecycle awareness. Scope asynchronous work to its owner, propagate cancellation and move blocking I/O off the main thread.
- Keep Activity references out of retained state. Document any change to state lifetime across configuration changes and process death.
- Key Compose effects to the state that should restart them. Use effect APIs for side effects; avoid writes during composition and stale captured callbacks.
- Read state that must trigger recomposition in composition. Report captured values from `SideEffect`; do not rely on a state read that occurs only inside it or behind a short-circuiting disabled-input condition.
- Keep DataStore updates transactional. Validate preferences and imported data before writing; preserve schema compatibility and the restore review/revalidation flow.
- Reuse the palette and settings components. Preserve full-row switch semantics, selected states, touch targets, font scaling and reduced/disabled motion.
- Add permissions, background work or dependencies only when the requested feature requires them. Keep core launcher features local and update checks manual.

For Home, app discovery, system integration or input transitions, also apply [nia-launcher-development](../nia-launcher-development/SKILL.md).

## Verify and finish

1. Exercise the changed behavior and its relevant failure/cancellation path. Prefer fakes and observable state or real UI interaction over implementation-shaped tests.
2. Use [CONTRIBUTING.md](../../../CONTRIBUTING.md#check-the-change) for test, lint and build commands. Inspect native renders for UI changes; use the device checklist for system behavior.
3. Apply the root version policy: bump both Gradle properties once per completed change set and update the changelog. Run the release-version validator.
4. Update the canonical guide when a boundary or product rule changes. Report checks and physical-device limits accurately.

## Primary references

- [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [Compose state hoisting](https://developer.android.com/develop/ui/compose/state-hoisting)
- [Compose side effects](https://developer.android.com/develop/ui/compose/side-effects)
- [Coroutine best practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
