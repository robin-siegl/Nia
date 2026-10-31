# Device testing

Use this checklist for the Pixel 10 Pro first, and record other devices separately. Automated tests are useful evidence for state and input behavior; they do not establish physical-device compatibility or SystemUI timing.

## Record the test environment

| Field | Record |
| --- | --- |
| Device | Model and any relevant display settings |
| Android | Version and build number |
| nia | Version, build type and tested commit |
| Navigation | Gesture or button navigation |
| Unlock | Face, fingerprint or PIN; tap-to-wake or power button |
| Conditions | Battery Saver, animation scale, font/display size and ambient display |
| Result | Pass/fail for the tested cases, with steps and captures for failures |

The primary phone is reported to run Android 17. Record what is actually installed instead of treating that target as a completed test result.

## Installation, setup and Home

- Install and open nia. Complete setup, choose nia in Android's Home dialog, and verify the Home gesture returns to it.
- Test all four setup stages, Back, Skip setup, Restore and All apps. Select an app mid-setup, close/reopen nia, and check the stage and assignments remain.
- Change the default launcher from nia Settings, both when nia is default and when it is not. The system chooser must remain available.
- Open every Settings page and return with Back. Scroll long pages: title and Back stay visible. Check overview summaries and tap both the label and control of each switch; each tap changes the setting once.
- Return Home from other apps and Recents. Old searches, keyboards and subpages must not remain in front of Home.
- Switch icon grid/text list. Keep Phone, Messages, Music, Calendar and Maps in order, with Apps as the sixth icon or matching text row.
- Open Apps by swipe and its visible entry. Open Settings through Apps and by holding empty Home space. Holding a Home app shortcut must not open Settings.

## Apps, assignments and search

- Assign all five categories to real apps, including an alternative messenger and navigation app. Confirm every category opens its chosen app.
- Tap time and date. Clock opens; date uses the Calendar assignment. Clear/reassign targets and check the unassigned behavior.
- Install, remove, disable or update an app while nia runs. The list refreshes, distinct launch entries remain identifiable, and unavailable assignments offer recovery.
- Search mixed case, accents, initials, camel-case initials and word prefixes in different orders. Results remain alphabetical. Clear restores the list; an empty result stays usable.
- Press Enter/Search with one result, several results and no results. Launch only the sole match. Test a physical keyboard if available.
- Toggle Keyboard on open. Focus and the real keyboard should appear only after Apps settles. Back first hides the keyboard, then closes Apps.
- Enable A–Z navigation. Jump to the first, middle and last letters, including in assignment pickers. Search hides it; clearing search restores it. Try TalkBack adjustment and keyboard arrows.
- Hold an app name for App info and left/right corner assignment. Check the actions while searching, dismiss the menu and repeat. Assignment retains the corner icon; pickers do not show this menu.

## Appearance and accessibility

- Test Black & white and Color in Light, Dark and System modes. Monochrome dark canvas is exactly `#000000`; colored dark mode can also use pure black.
- Pick presets, tap/drag the color wheel, adjust brightness and enter hex values. Both theme previews update; wheel/slider choices persist after release. Invalid hex cannot be applied.
- Change Android's theme while nia is open and while another app is foregrounded. System follows it; explicit Light/Dark stays selected.
- Test large font and display scaling on Home, Apps, Settings and dialogs. Keep all actions reachable and text readable, including the last restore change and reset controls.
- Check status/navigation contrast, camera cutouts, landscape and keyboard insets. Try TalkBack labels, switch/choice states and focus order. Hidden Home controls must not be accessible beneath Apps.
- Toggle Pixel-style clock, compare the intended portrait reference, and rotate to landscape and back. In Pixel mode, app text size must not move the clock/date. Classic mode keeps scalable header text.
- Change time zone and 12/24-hour preference, and check time/date updates at midnight.

## Drawer, corners and unlocks

- Drag Apps slowly, stop midway, reverse direction and release. Follow the finger, settle cleanly and avoid stray launches. Compare tap, swipe and Back paths.
- Scroll down Apps, then swipe down. Scroll the list rather than closing it until it reaches the top. Repeat with search results and an empty list.
- Interrupt a drag/settle with the notification shade, Android Home or screen lock. On return, the next tap and swipe work without waiting for a clock tick or settings change.
- Configure Empty/App in both corners and try icons throughout the chooser. Settings is not offered. Relaunch/reboot, then uninstall an assigned app and verify recovery.
- Upgrade a legacy Settings corner: it becomes empty. A legacy gear icon on an app becomes Star while retaining the app.
- Lock from nia Home, then unlock with face, fingerprint and PIN. The header stays stationary while shortcuts reveal. Lock from another app, unlock there and return Home; no delayed reveal should replay.
- **Compare tap-to-wake and the power button using the same lock and unlock methods.** Repeat after screen timeout and with ambient display on/off. Record whether a missed animation leaves shortcuts already visible and responsive or hidden with blocked input.
- Interrupt an unfinished reveal with Quick Settings or a focus change. Rotate/change system appearance during unlocking. The reveal should finish when Home is active, including after activity recreation; a completed reveal must not replay on later focus changes.
- Repeat warm/cold starts. Record a screen capture and OS build if the header jumps or the animation disappears. Android controls the system lock-screen exit; nia does not own that transition.
- Test Standard, Reduced and Android animation scale zero. Change the scale during drawer/reveal motion. Completion or cancellation must restore input.
- With Battery Saver on and off, test Music, Clock/date, a corner, Apps and swipe-up immediately after settling. Toggle Battery Saver from Quick Settings during a transition and repeat on return. Record phone results separately from simulated scale tests.
- Double-tap launch targets quickly. Open one activity without leaving navigation stuck. Toggle Android touch haptics and check press feedback follows that preference.

## Backups, recovery and upgrades

- Reboot and force-stop/reopen nia. Verify layout, text size, app choices, corners, animations, colors and app-list preferences persist.
- Save a backup, change settings, then restore it. Preview each changed current/backup value and omit unchanged entries. Cancel first and confirm nothing changes; reopen and apply.
- Import supported schema 1–5 backups. Check defaults for older fields, Settings-corner migration and current exports with custom corners and Reduced animations.
- Cancel file selection. Try malformed, oversized, unsupported-version and unsupported-profile files. Keep existing settings intact.
- Restore a backup containing unavailable apps. List the affected assignments and clear only those targets while restoring valid settings.
- Remove an app while its restore preview is open. Restore must refresh the preview and require another review. Rapid duplicate Restore taps must not apply twice.
- Cancel both reset confirmations. Reset appearance & behavior keeps everyday apps, Clock and both corner apps/icons; Reset all settings returns to setup.
- With unreadable saved settings, keep Restore backup and full reset available while saving a backup and partial reset remain disabled.
- Install over an earlier production release using the same signing key. Confirm settings survive. A debug install is a separate app, not this upgrade test.

## About and update checks

- Verify the installed version, privacy text and project/issue/notices links.
- Tap Check for updates. Check the latest stable version, APK link and release page; opening About alone must not make a request.
- Test offline/retry, rate limiting or no release where feasible, equal/newer-installed versions, and a release without an APK. A missing APK still offers the release page.
- Confirm downloads require a tap and open externally, with installation handled by Android.

## Supported scope

Test the personal profile. Work profiles and Private Space are not currently supported. A Pixel-only pass does not establish broad manufacturer, tablet or foldable compatibility. Keep untested cases marked as untested.
