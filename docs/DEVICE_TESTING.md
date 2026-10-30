# Pixel 10 Pro acceptance

Record the Android version, build number and gesture/button navigation setting in the test results. Android 17 is the reported intended baseline, not a claim that this repository has already been tested on that physical phone.

## Home and navigation

- Install the APK, open nia, complete or skip setup, then choose nia in Android's default Home dialog.
- From several other apps, use the Home gesture. nia should return to its clean Home, without an old search or keyboard.
- Open Apps using both swipe-up and the visible Apps entry. Confirm swiping does not interfere with Android Home/Back gestures or the notification shade.
- Test normal Recents switching and returning to Home. Record any OS-controlled transition differences.
- Switch between icon grid and text list. Confirm five fixed categories and no rearrangement/add controls.
- Try large system text/display scaling. Keep Apps, settings and every category reachable.

## Apps

- Assign all five categories. Try an SMS app/WhatsApp, your music app, dialer, calendar and navigation app.
- Tap the time and date. Confirm Clock opens and date uses the same Calendar assignment.
- Install/uninstall an app while nia runs. Confirm the list refreshes and stale assignments recover through the app selector.
- Search with mixed case, accents, no results and a blank query. Clear should restore alphabetical order.
- Enable/disable autofocus, reopen Apps each time, and test the real keyboard. Back first dismisses the keyboard, then leaves the list.
- Long-press an app name to open App info.

## Appearance

- Check black/white Light, Dark and System behavior. Monochrome dark canvas must be exactly black.
- Select violet and other seed colors. Check both generated previews and readable text throughout Home, Apps and settings.
- Change system appearance while nia is foregrounded and while another app is open. System mode should follow it; explicit Light/Dark should hold their choice.
- Enable colored OLED mode. Confirm pure black with the generated light foreground in dark mode.
- Confirm status/navigation controls remain legible and the camera cutout/keyboard do not overlap content.
- Change time zone and 12/24-hour preference; verify date and time update correctly, including at midnight.

## Persistence and recovery

- Reboot and force-stop/reopen nia. Check layout, assignments, colors and autofocus.
- Export settings, change them, then import. Review the restore summary and confirm the restored result.
- Cancel import; try malformed and oversized files. Existing settings must remain intact.
- Import a valid backup referencing missing apps. Other preferences restore; unavailable assignments are listed for reassignment.
- Check reset appearance keeps assignments. Full reset should require an explicit reset choice and return to setup.
- Install an update signed with the same release key and verify all preferences survive.

## Boundaries

Personal profile only. Work profiles and Private Space are not part of this MVP acceptance. Broad manufacturer/tablet compatibility is not claimed from a Pixel-only pass.
