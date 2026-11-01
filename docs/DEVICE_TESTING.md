# Device testing

Use this checklist for the Pixel 10 Pro first, and record other devices separately. Automated tests are useful evidence for state and input behavior; they do not establish physical-device compatibility or SystemUI timing.

## Dev APK downloads

- Run Dev APK from a repository branch, wait for all checks and download its artifact while signed into GitHub. Extract and install the APK; verify the checksum and source/build information in the bundle.
- Keep nia and nia Dev installed together. Switch the default Home app between them, complete dev setup and import a nia settings backup. Confirm that each installation keeps its own settings.
- Install another signed dev build with the same base version/code. Check that it updates nia Dev without uninstalling or resetting its setup, Home shortcuts or corners.
- Check the nia Dev label and About version/run/commit, including large text and both themes. Public-release links should clearly identify their separate channel.
- Test the optimized dev build's Home, search, gestures and unlock behavior. Record actual phone results separately from automated package/signature checks.

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
- Test all four setup stages, Back, Skip setup, Restore and All apps. Your shortcuts can skip to an empty Home, without inserting the old five categories. Select an app mid-setup, close/reopen nia, and check the stage and assignments remain.
- Change the default launcher from nia Settings, both when nia is default and when it is not. The system chooser must remain available.
- Open every Settings page and return with Back. Scroll long pages: title and Back stay visible. Check overview summaries and tap both the label and control of each switch; each tap changes the setting once.
- Return Home from other apps and Recents. Old searches, keyboards and subpages must not remain in front of Home.
- Switch icon grid/text list with zero, one and nine app/command shortcuts. Preserve the chosen order, names, icons and assignments; There is no generated Apps entry. An optional Open Apps command can move anywhere. The three-by-three grid never scrolls.
- With Android auto-rotate enabled, turn the phone sideways and upside down on Home, Apps, setup, Settings, app pickers and dialogs. nia stays upright in portrait. Return Home from a landscape app and repeat after locking/unlocking; other apps keep their own orientation behavior.
- Open Apps by swipe, its accessibility action and an optional configured command. Open Settings through Apps and by holding empty Home space. Holding a Home app shortcut must not open Settings.

## Apps, assignments and search

- Add, rename, reassign, change icons, reorder and remove Home apps in Settings → Home shortcuts. Try Google Maps with the Maps icon, then another app without losing its chosen name/icon. At nine shortcuts, Add shortcut and app-menu Add to Home are disabled; App info and corner assignment remain available.
- Tap time and date. Clock opens; date uses its independent Calendar assignment. Remove the Home Calendar shortcut and repeat. Clear/reassign targets and check the unassigned behavior.
- Install, remove, disable or update an app while nia runs. The list refreshes, distinct launch entries remain identifiable, and unavailable assignments offer recovery.
- Search mixed case, accents, initials, camel-case initials and word prefixes in different orders. Results remain alphabetical. Clear restores the list; an empty result stays usable.
- Press Enter/Search with one result, several results and no results. Launch only the sole match. Test a physical keyboard if available.
- Toggle Keyboard on open. Focus and the real keyboard should appear only after Apps settles. Back first hides the keyboard, then closes Apps.
- Enable A–Z navigation. Jump to the first, middle and last letters, including in assignment pickers. Search hides it; clearing search restores it. Try TalkBack adjustment and keyboard arrows.
- Hold an app name for Add to Home, App info and left/right Quick corner assignment. Add to Home opens a prefilled draft; Cancel saves nothing and Save returns Home with the shortcut. Check the actions while searching, dismiss the menu and repeat. Assignment retains the corner icon; pickers do not show this menu.

## Appearance and accessibility

- Test Black & white and Color in Light, Dark and System modes. Monochrome dark canvas is exactly `#000000`; colored dark mode can also use pure black.
- Pick presets, tap/drag the color wheel, adjust brightness and enter hex values. Both theme previews update; wheel/slider choices persist after release. Invalid hex cannot be applied.
- Change Android's theme while nia is open and while another app is foregrounded. System follows it; explicit Light/Dark stays selected.
- Test large font and display scaling on Home, Apps, Settings and dialogs. Keep all actions reachable and text readable, including the last restore change and reset controls.
- Check status/navigation contrast, camera cutouts, system-controlled window resizing and keyboard insets. Try TalkBack labels, switch/choice states and focus order. Hidden Home controls must not be accessible beneath Apps.
- Toggle Pixel-style clock and compare the intended portrait reference. Turning the phone sideways must not change nia's orientation. In Pixel mode, app text size must not move the clock/date. Classic mode keeps scalable header text.
- Change time zone and 12/24-hour preference, and check time/date updates at midnight.

## Drawer, corners and unlocks

- In **both grid and list**, pull up slowly. Check that the same clock/date shrinks continuously while shortcuts move upward, followed by the resisted drawer preview. Show no pull text or progress ring, and give one threshold haptic only. Release after the threshold to open; reverse or release early to cancel. Compare tapping Apps and Back.
- With eight apps plus an optional Apps command, Large text and maximum system font/display size, scroll all nine entries before entering the pull. Apps must share the scroll viewport and corners remain reachable. A short fast fling or momentum reaching the end must never arm/open Apps in either layout.
- Cancel an armed drag with Android's gesture cancellation or a focus interruption. The next real tap/swipe must work. Closing Apps restores the previous scroll/header position; explicit Home and a new unlock reset to the top before the reveal.
- Recheck with touch haptics disabled, Reduced motion and animation scale zero. Feedback must not repeat as the finger jitters near the threshold, and no launch may depend on haptics.
- Scroll down Apps, then swipe down. Scroll the list rather than closing it until it reaches the top. Repeat with search results and an empty list.
- Interrupt a drag/settle with the notification shade, Android Home or screen lock. On return, the next tap and swipe work without waiting for a clock tick or settings change.
- Configure Empty/App/Command in Quick corners and try icons throughout the chooser. Settings is not offered. Relaunch/reboot, then uninstall an assigned app and verify recovery.
- Upgrade a legacy Settings corner: it becomes empty. A legacy gear icon on an app becomes Star while retaining the app.
- Lock from nia Home, then unlock with face, fingerprint and PIN. The header stays stationary while shortcuts reveal. Lock from another app, unlock there and return Home; no delayed reveal should replay.
- **Compare tap-to-wake and the power button using the same lock and unlock methods.** Repeat after screen timeout and with ambient display on/off. Record whether a missed animation leaves shortcuts already visible and responsive or hidden with blocked input.
- Interrupt an unfinished reveal with Quick Settings or a focus change. Rotate/change system appearance during unlocking. The reveal should finish when Home is active, including after activity recreation; a completed reveal must not replay on later focus changes.
- Repeat warm/cold starts. Record a screen capture and OS build if the header jumps or the animation disappears. Android controls the system lock-screen exit; nia does not own that transition.
- Test Standard, Reduced and Android animation scale zero. Change the scale during drawer/reveal motion. Completion or cancellation must restore input.
- With Battery Saver on and off, test Music, Clock/date, a corner, Apps and swipe-up immediately after settling. Toggle Battery Saver from Quick Settings during a transition and repeat on return. Record phone results separately from simulated scale tests.
- Double-tap launch targets quickly. Open one activity without leaving navigation stuck. Toggle Android touch haptics and check press feedback follows that preference.

## Reordering and commands

- Long-press drag Home icons across rows and list entries past one another. Repeat in Settings → Home shortcuts and with maximum text/display scaling. Neighbors show the preview, but only dropping saves it. Cancel, leave the screen or lock during the gesture: keep the saved order and restore normal taps/swipes.
- Drag toward the top/bottom of long Home and Settings lists. Scroll the list and reach off-screen positions without starting the Apps gesture. Reorder the optional Apps command like any other entry. Check the Move up/down alternatives with TalkBack and a keyboard.
- Open Apps and Search apps from Home and both corners. Search apps focuses only after settling and does not change Keyboard on open. A later ordinary opening follows the saved preference.
- Toggle flashlight repeatedly and change its state through Quick Settings between taps. Test a busy camera, absent flash and process/activity recreation. Report unavailable hardware without crashing; add no camera permission prompt or camera session.
- Start a timer from Home and either Quick corner. Open the Clock app's duration entry without a preset or automatic countdown, choose a duration there, then return Home. Let Clock ring and cancel the timer there, including after nia is stopped. Try multiple compatible Clock apps, a disabled/missing handler and an immediate retry after failure. Keep existing assignments and show a useful error if no app can open. No exact-alarm access or runtime permission prompt should appear in nia.
- Set an alarm from Home and both corners: Clock shows time entry, with no preset alarm time. Start several preset timers using 5/15/25 minutes and custom values, including 1 minute and 24 hours. Verify each corner's independent duration, cancellation in the shared picker, and Clock's timer alerts after nia is stopped.
- Toggle nia's theme from System in both Android light/dark modes, then repeat from explicit Light/Dark. Preserve the seed and pure-black setting, persist through restart and reselect System in Appearance. Android's theme must stay unchanged.
- Open Sound controls and change volume in Android's panel, then return to nia. Check the sound-settings fallback where the panel is unavailable, and a useful error plus immediate retry if both cannot open.
- Export and restore mixed app/command shortcuts and corners. Preview command changes, preserve names/icons/order, and reject unknown command IDs, simultaneous app/command targets and command fields disguised as old schema versions.

## Icon picker

- Open it from a Home app and from either Quick corner. Search names, category words and synonyms such as controller, GPS, Wi-Fi and cafe. Browse categories; typing a search must cover the full catalog.
- Preview a different icon, then Cancel or Close: the original selection stays saved. Confirm a choice and reopen it, checking the same icon/name/target after restart and backup restore.
- Recreate the activity during icon selection. Preserve the pending icon and query without applying them early. Use Android Back with the keyboard open, then dismiss the picker.
- Check light/dark and colored themes, exact OLED black, system bars, maximum text/display size and keyboard insets. Keep search, confirmation and Close reachable. Category browsing remains available at large text sizes when there is room.
- Test TalkBack labels/selected state and keyboard focus. Icons and license assets are bundled; airplane mode must not affect browsing or selection.

## Backups, recovery and upgrades

- Reboot and force-stop/reopen nia. Verify layout, text size, app choices, corners, animations, colors and app-list preferences persist.
- Save a backup, change settings, then restore it. Preview each changed current/backup value and omit unchanged entries. Cancel first and confirm nothing changes; reopen and apply.
- Import supported schema 1–8 backups. Check that schema 7 commands still import without preset parameters and that duration-only changes appear in the restore preview. Check defaults for older fields, Settings-corner migration and current exports with custom corners and Reduced animations.
- Cancel file selection. Try malformed, oversized, unsupported-version and unsupported-profile files. Keep existing settings intact.
- Restore a backup containing unavailable apps. List the affected assignments and clear only those targets while retaining Home names/icons/order and restoring valid settings.
- Remove an app while its restore preview is open. Restore must refresh the preview and require another review. Rapid duplicate Restore taps must not apply twice.
- Cancel both reset confirmations. Reset appearance & behavior keeps Home names/icons/order/targets, Clock, Calendar and both corner app/command targets and icons; Reset all settings returns to setup.
- With unreadable saved settings, keep Restore backup and full reset available while saving a backup and partial reset remain disabled.
- Install over an earlier production release using the same signing key. Confirm the five old categories become editable entries with their targets preserved and completed setup does not repeat. Restore/save an explicitly empty Home, restart and reset appearance: it must stay empty. Confirm settings survive. A debug install is a separate app, not this upgrade test.

## About and update checks

- Verify the installed version, privacy text and project/issue/notices links.
- Tap Check for updates. Check the latest stable version, APK link and release page; opening About alone must not make a request.
- Test offline/retry, rate limiting or no release where feasible, equal/newer-installed versions, and a release without an APK. A missing APK still offers the release page.
- Confirm downloads require a tap and open externally, with installation handled by Android.

## Supported scope

Test the personal profile. Work profiles and Private Space are not currently supported. A Pixel-only pass does not establish broad manufacturer, tablet or foldable compatibility. Keep untested cases marked as untested.
