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
- Configure both corners as Empty, Settings and apps. Pick several outline icons, relaunch/reboot, and verify app names are spoken by TalkBack. Uninstall a corner app and tap its shortcut to recover through app selection.
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
- Long-press an app name to open App info.

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
- Export settings, change them, then import. Review the restore summary and confirm the restored result.
- Import schema-1/2/3 backups. Existing preferences should remain, corners should default empty and motion should follow Android. Export/re-import schema 4 with two custom corners and Reduced motion.
- Cancel import; try malformed and oversized files. Existing settings must remain intact.
- Import a valid backup referencing missing apps. Other preferences restore; unavailable assignments are listed for reassignment.
- Check reset appearance keeps assignments. Full reset should require an explicit reset choice and return to setup.
- Install an update signed with the same release key and verify all preferences survive.

## Boundaries

Personal profile only. Work profiles and Private Space are not part of this MVP acceptance. Broad manufacturer/tablet compatibility is not claimed from a Pixel-only pass.
