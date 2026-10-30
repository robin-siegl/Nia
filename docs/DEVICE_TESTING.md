# Pixel 10 Pro acceptance

Record the Android version, build number and gesture/button navigation setting in the test results. Android 17 is the reported intended baseline, not a claim that this repository has already been tested on that physical phone.

## Home and navigation

- Install the APK, open nia, complete or skip setup, then choose nia in Android's default Home dialog.
- Test all four setup stages, Back, Skip setup, restore and All apps. Select apps mid-setup, reopen nia, and confirm its current stage and assignments remain. The final preview should match the selected layout/theme.
- Open every Settings page and return using Back. App selection must return to its originating page. Change the default launcher from the overview both while nia is default and while another launcher is default.
- From several other apps, use the Home gesture. nia should return to its clean Home, without an old search or keyboard.
- Open Apps using both swipe-up and the visible Apps entry. Confirm swiping does not interfere with Android Home/Back gestures or the notification shade.
- Test normal Recents switching and returning to Home. Record any OS-controlled transition differences.
- Switch between icon grid and text list. Confirm five fixed categories and no rearrangement/add controls.
- Confirm Apps is the sixth grid icon or a text row matching the other entries. Open Settings through Apps and by holding blank Home space. Holding an app shortcut must not open Settings.
- Select Small / Default / Large Home text. Verify the preview, clock, date and list entries; check that configured bottom corners stay reachable while a long Home list scrolls.
- Try large system text/display scaling. Keep Apps, settings and every category reachable.
- Compare the Pixel clock layout with the reference lock screen after unlocking. Check both grid and list, time/date tap actions, the spaced time, date weight and screen anchors. Toggle Pixel clock layout off to verify the old scalable header; rotate to landscape and back.
- With Pixel clock layout enabled, changing app text sizes should not move the clock/date. Check a signed upgrade keeps the selected layout and assignments, and older backups enable the aligned header with the old layout still selectable.

## Motion and corners

- Slowly pull Apps up, pause midway, reverse the gesture and release. It should track the finger, cancel without a stray launch, and settle according to distance/velocity. Tap Apps and Back for the equivalent transitions; test Android Home while a transition runs.
- Scroll far down Apps and swipe down: scroll the list instead of dismissing it. Pull down from its top and from an empty/search-results drawer to dismiss. Confirm the alphabet scrubber still works.
- Enable autofocus: the keyboard must appear after the drawer settles. Close/reopen with and without the keyboard. Test rapid extra taps during transitions; no hidden Home shortcut or unintended app should launch.
- Lock from Home, unlock by face/fingerprint/PIN, and observe the unchanged header plus shortcut-only reveal. Return from several normal apps without locking; no reveal should replay. Unlock into another app, then return to nia; it must not replay a delayed reveal. Check process recreation and rotation.
- Configure both corners as Empty and App; Settings must not be offered. Pick several of the 27 outline icons (including scrolling the icon chooser), relaunch/reboot, and verify app names are spoken by TalkBack. Uninstall a corner app and tap its shortcut to recover through app selection.
- Upgrade from 1.2 with a Settings corner: that corner becomes empty and all other settings/assignments remain. A legacy gear icon assigned to an app becomes Star while retaining the app.
- Test face/fingerprint/PIN unlock with nia both warm and recreated. The 1.3 reveal waits briefly with only the header visible, then fades/rises the shortcuts; check the animation remains visible after the system fade. Lock/unlock from another app, including a quick return Home, and from nia Settings/Apps. Record OS build and a screen recording if the header still jumps or the reveal is skipped. The system clock exit is not controlled by nia.
- Compare Follow Android and Reduced motion. With Android animation scale set to zero, transitions must finish immediately without leaving input blocked. Toggle system touch haptics and check shortcut feedback follows that setting.
- Tap a launchable shortcut rapidly twice; open one activity, without leaving navigation stuck. Check Settings stays reachable with both corners empty.

## Apps

- Assign all five categories. Try an SMS app/WhatsApp, your music app, dialer, calendar and navigation app.
- Tap the time and date. Confirm Clock opens and date uses the same Calendar assignment.
- Install/uninstall an app while nia runs. Confirm the list refreshes and stale assignments recover through the app selector.
- Search with mixed case, accents, no results and a blank query. Clear should restore alphabetical order.
- Search app initials, camel-case initials and multiple word prefixes in different orders. Matches must stay alphabetical.
- Search for exactly one app and press keyboard Search/Enter; it should open. With several matches or none, it must not launch an arbitrary app. Test a physical keyboard if available.
- Open About nia and verify the version, privacy text and browser links for the project, issue reporting and notices.
- Enable/disable autofocus, reopen Apps each time, and test the real keyboard. Back first dismisses the keyboard, then leaves the list.
- Enable the alphabet scrubber and drag the right edge to the first, middle and final letters. Check the same behavior in assignment pickers, accent normalization and search hiding/restoring the scrubber. Try TalkBack adjustment and keyboard arrows.
- Long-press an app name: show App info, Assign to left corner and Assign to right corner without launching the app. Verify each action, dismiss the menu, and repeat while searching. Assigning keeps the previous corner icon. App selection screens must not show the action menu.
- In About, tap Check for updates. Check the latest stable version, APK link and release-page link. Test offline/retry, no release/rate limiting where feasible, same version and a newer development version. A release without an APK should still offer its page. Opening About/Home must not start a request by itself, and downloads must require a tap and stay in the browser/Android installer.

## Appearance

- Check black/white Light, Dark and System behavior. Monochrome dark canvas must be exactly black.
- Select violet and other seed colors. Check both generated previews and readable text throughout Home, Apps and settings.
- Tap and drag the color wheel, adjust brightness, and type a custom hex value. Both previews should update immediately, and wheel/slider selections should persist after release. Invalid hex must not save. Test scrolling around the wheel and TalkBack hue/saturation actions.
- Change system appearance while nia is foregrounded and while another app is open. System mode should follow it; explicit Light/Dark should hold their choice.
- Enable colored OLED mode. Confirm pure black with the generated light foreground in dark mode.
- Confirm status/navigation controls remain legible and the camera cutout/keyboard do not overlap content.
- Change time zone and 12/24-hour preference; verify date and time update correctly, including at midnight.

## Persistence and recovery

- Reboot and force-stop/reopen nia. Check layout, text size, assignments, corners, motion, colors, autofocus and alphabet navigation.
- Update from 0.1.0 with the same signing key. Existing settings and older exports should load with Default text size and the alphabet scrubber off.
- Export settings, change them, then import. Review each changed setting and app assignment with its current/backup value; unchanged entries should be omitted. Scroll to the last entry with large system text. Cancel, verify no changes, reopen and restore the exact previewed result.
- Import schema-1/2/3 backups. Existing preferences should remain, corners should default empty and motion should follow Android. Import schema 4 with both app and Settings corners, checking migration. Export/re-import schema 5 with two custom corners, new icons and Reduced motion.
- Cancel import; try malformed and oversized files. Existing settings must remain intact.
- Import a valid backup referencing missing apps. Other preferences restore; unavailable assignments are listed for reassignment.
- Uninstall an assigned app while a restore preview is open, then tap Restore. nia must refresh the preview and require a second review; rapid duplicate Restore taps must not apply twice.
- Check reset appearance keeps assignments. Full reset should require an explicit reset choice and return to setup.
- Install an update signed with the same release key and verify all preferences survive.

## Boundaries

Personal profile only. Work profiles and Private Space are not part of this MVP acceptance. Broad manufacturer/tablet compatibility is not claimed from a Pixel-only pass.
