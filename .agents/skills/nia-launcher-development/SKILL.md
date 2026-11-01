---
name: nia-launcher-development
description: >-
  Build and diagnose nia's Android launcher behavior. Use for default Home selection, app discovery and launch targets, package changes, app search, commands, reordering, drawer gestures, keyboard and navigation, lock/unlock animations, Battery Saver input failures, app assignments or work/private-profile proposals. Apply alongside nia-android-development.
---

# Develop reliable launcher behavior

## Trace the real path

Read [AGENTS.md](../../../AGENTS.md), the [architecture guide](../../../docs/ARCHITECTURE.md), [design guide](../../../docs/DESIGN.md) and [Android skill](../nia-android-development/SKILL.md).

Trace the interaction from the Android callback or pointer event through its state owner to visible UI and the launch/navigation action. For an intermittent bug, record the triggering sequence and distinguish a missing visual effect from blocked input. Preserve user work and keep investigation scoped to the reported behavior.

## Apply platform and product constraints

- Use the Home role and system chooser. Preserve manifest HOME/DEFAULT and launcher entry points, `singleTask` behavior and clean `onNewIntent` handling.
- Keep app identity as a launch component in the current personal profile. Preserve distinct launcher aliases, filter synthetic App-info entries, handle package callbacks and unregister listeners.
- Use `LauncherApps` for launching and App info. Handle unavailable activities without losing access to Home, Apps or Settings. Keep backup targets as validated identities, never arbitrary intents.
- Keep the full app list alphabetical and text-only. Follow the active Home requirements in AGENTS.md: up to nine editable app/command shortcuts, no reserved Apps entry, and optional setup that can leave Home empty. Preserve legacy layouts through migration rather than retaining immutable categories. Keep header Clock/Calendar assignments independent and retain optional Empty/App/Command corners.
- Morph one clock/date header and move Home content with its changing height. Consume header movement, list scrolling and the deliberate Apps pull in that order; use one threshold haptic and no visible pull instructions.
- Keep reordering independent of the Apps gesture. Test real long-press drags across rows, edge scrolling, drop, cancel and stale-data rejection, with one saved update per drop.
- Keep parent pointer handlers stable across child gestures. Measure reorder slots separately from lifted content, and check that the visible dragged item follows the finger rather than only asserting the saved order.
- Dispatch only the supported typed commands. Use the public torch API without opening the camera, observe actual torch changes and release its callbacks.
- Coordinate drawer dragging, list scrolling, keyboard focus and Android navigation. Focus search after settling; launch with Enter only for a sole result. Test tap, swipe, Back and external Home paths.
- Treat lock state, interactivity, focus and lifecycle as separate observations. Re-read current state when a deferred broadcast arrives; do not assume callback order or that a lit display is interactive.
- Preserve a queued reveal until visible completion. Retain it across activity recreation, replace unfinished requests on focus interruption, and cancel on a real departure. Keep the header stationary and avoid replay on normal app returns.
- Release touch blocking when a transition finishes or is canceled, including at zero animation scale. Keep completion and scrollability subscribed in composition; do not rely on a minute tick, timer or Battery Saver toggle to restore input.
- Keep ordinary launcher behavior independent of accessibility services, overlays, device administration and battery exemptions. Do not use private SystemUI APIs or privileged permissions to simulate control of the lock-screen exit.
- Treat work profiles and Private Space as separate future features. Check the complete current Android contract, including identity, locked/paused visibility, search exclusion and backup behavior, before expanding scope.

## Validate the behavior

Use relevant cases from [DEVICE_TESTING.md](../../../docs/DEVICE_TESTING.md). For a lifecycle/input change, cover the relevant subset of:

- Resume before focus and focus before resume; duplicate or delayed broadcasts.
- Pause/stop, recreation and an explicit departure during a reveal.
- Gesture cancellation and repeated taps while input is blocked.
- Standard, Reduced and disabled animations; a duration-scale change mid-transition.
- Real taps and swipes immediately after settling, with a fixed clock in tests.
- Physical tap-to-wake versus power-button wake, using the same unlock method.

Inject animation duration scale into the Compose test effect context when testing it. Treat Robolectric state sequences as simulations; verify Pixel/SystemUI timing on the phone before claiming a device fix. Avoid guessed delays or extra permissions as a substitute for evidence.

Apply the root version/changelog policy once for the completed change set. Record remaining physical checks and update the canonical architecture/design guide if behavior changes.

## Primary references

- [Home role](https://developer.android.com/reference/android/app/role/RoleManager#ROLE_HOME)
- [LauncherApps](https://developer.android.com/reference/android/content/pm/LauncherApps)
- [Package visibility declarations](https://developer.android.com/training/package-visibility/declaring)
- [Activity lifecycle and focus callbacks](https://developer.android.com/reference/android/app/Activity)
- [Broadcast delivery](https://developer.android.com/develop/background-work/background-tasks/broadcasts)
- [PowerManager interactivity](https://developer.android.com/reference/android/os/PowerManager#isInteractive())
- [Private Space launcher requirements](https://developer.android.com/about/versions/15/behavior-changes-all#private-space)
