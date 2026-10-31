# Pixel lock-screen alignment

The portrait header is calibrated against the supplied 688 × 1536 Pixel 10 Pro lock-screen and Home screenshots. The original screenshots are not distributed in this repository.

The [rendered Home capture](screenshots/home-pixel-reference.png) shows the resulting layout without system bars.

| Visible landmark | Left | Top | Width | Height |
| --- | ---: | ---: | ---: | ---: |
| Clock sample `14 03` | 46 | 186 | 272 | 89 |
| Date sample `Fri, 30 Oct` | 54 | 350 | 126 | 23 |

These are measured text-ink bounds in reference pixels, not Android view bounds. Anchors scale with window width. The Home surface subtracts its already-applied safe-drawing top inset once, so the clock/date occupy the intended full-screen positions. App shortcuts begin below the date's touch target. Optional shortcuts occupy the bottom corners; Settings is always available in Apps and by holding blank Home space.

The header uses locally bundled Google Sans-derived Regular and SemiBold fonts with calibrated ink sizes and widths. OLED monochrome uses the reference's soft clock and white date; other themes retain their shared foreground color. Time and date retain their Clock and Calendar tap actions, spoken labels and at least 48dp touch targets.

## Appearance controls

Settings → Home → Pixel clock layout selects the aligned portrait header. Its clock/date metrics stay independent of Home app text sizes. Turning it off restores the previous scalable clock/date behavior. Landscape uses the compact layout. The setup and Settings previews reflect this choice.

This matches the supplied clock style and settings. nia does not read or automatically track Pixel's lock-screen clock selection, width slider, animation state or future OS layout changes. A different system clock style may need a different calibration.

## Unlock motion

nia listens for screen/keyguard state while its activity exists. Version 1.3.1 retains the unlock session in the ViewModel across activity recreation. An observed lock survives a late stop callback that reports an already-unlocked screen before `USER_PRESENT`. A pending handoff has a 1.5-second background grace period; once nia resumes within it, a slow window-focus handoff no longer expires that unlock. An explicit user departure discards both the candidate and reveal; an unlocked activity stop cancels any already-created reveal so ordinary app returns do not replay it.

The reveal request survives settings loading and stays pending until the animation finishes. It runs only while nia's window is resumed, focused, interactive and unlocked. Losing focus or pausing requeues unfinished work with a new identity, so completion of an earlier effect cannot consume the replacement even if pause/resume occur between drawn frames. A configuration change preserves that replacement; leaving for another app cancels it. Deferred screen-off broadcasts are checked against current keyguard/power state before hiding Home.

Home first draws its stationary header, then uses a 120ms animation-scaled lead-in and 360ms row fades with up to 16dp upward movement and 24ms row staggering. The lead-in remains a heuristic, not a system callback for the end of the keyguard animation. Android documents that [focus and lifecycle callbacks have no guaranteed relative order](https://developer.android.com/reference/android/app/Activity#onWindowFocusChanged(boolean)) and that [broadcast delivery may be deferred](https://developer.android.com/develop/background-work/background-tasks/broadcasts).

Reduced motion uses a 120ms fade with no lead-in, movement or stagger. Compose animations inherit Android's duration scale, including zero-duration animations. The two clocks remain separate system/app views: nia does not own or replace Android's lock-screen exit animation. A cold process created after the unlock broadcast may not observe that unlock. Warm/cold unlocks, biometric unlock and the visual handoff require validation on the physical Pixel; native UI tests exercise the event ordering and stationary header, not Pixel SystemUI.

### Home input recovery

Reveal completion must be read in composition even while the parent disables input, and its captured Boolean is reported to the parent. Previously the input-enabled condition short-circuited this read, leaving the completion state read only inside `SideEffect`. The icons could finish appearing while the parent's invisible touch blocker stayed active until another Home recomposition, such as a clock update.

Regression tests render the actual Home/drawer container with a fixed clock and use real pointer events for category, Clock, corner, Apps and swipe actions. They cover grid/list unlocks, reduced motion, zero animation scale, switching scale during the reveal and drawer transitions, and a cancelled swipe. The animation scale is injected into Compose's effect context; these tests do not claim to reproduce Pixel's complete Battery Saver policy. Battery Saver was not required to reproduce the input freeze. This fix adds no permission, battery exemption, polling or recovery timer.

## Permissions and reliability

Select nia as the default Home app through Android's Home role. No accessibility service, device administrator, notification access, overlay, root or battery-optimization exemption is needed for the implemented launcher behavior. nia has no always-running service or background polling loop.

The platform [permission definitions](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/res/AndroidManifest.xml) restrict remote transition control (`CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS`, signature/privileged/recents) and direct keyguard-state subscription (`SUBSCRIBE_TO_KEYGUARD_LOCKED_STATE`, signature/module/role, intended for Assistant). These cannot be gained by adding an ordinary runtime permission request to nia. Internet permission is separate and used only for the requested manual GitHub update check.

## Weather status

The reference contains two separate weather presentations: the large outline complication attached to the system clock, and the small colored weather/temperature beside the date. Neither is synthesized in nia.

Android's [Smartspace manager](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/app/smartspace/SmartspaceManager.java) is a system API. The platform [permission definitions](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/res/AndroidManifest.xml) restrict its management/access, and the [clock weather data flow](https://android.googlesource.com/platform/frameworks/base/+/4c0f7a37669d1dc982045a6b07ac92127cfb5761%5E!/) is inside SystemUI. These are not a supported public component ordinary launchers can embed.

Google's public Weather API publishes weather-condition images, but it does not supply the Pixel lock-screen clock component. Exact Pixel weather integration remains deferred until a supported data and icon source can meet the requested appearance. No weather requests, location permission, placeholder condition or generic weather symbol has been added. Internet permission is solely for manual update checks.
