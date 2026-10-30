# Changelog

## 1.3.0

- App-name long presses now offer App info and assignment to either corner, keeping the chosen icon and fixed Home categories.
- About can manually check the latest public GitHub release and open its APK or release page. Added loading, retry, rate-limit, no-release, current-version and missing-APK states. Internet permission is used only for this requested check; no automatic checks/downloads or install permission.
- Restore previews each changed preference and app assignment with current/backup values, lists unavailable apps and rechecks the result before applying. Duplicate restore submissions are blocked.
- Removed Settings as a corner action and expanded the matching outline icon collection from 12 to 27. Upgrades/older imports clear legacy Settings corners and replace legacy app gear icons with Star while preserving other preferences.
- Fixed pending unlocks lost when user-present precedes resume and reveal requests lost before Home first composes. Reveals are consumed once across Home remounts, use a scaled lead-in and a longer fade/rise, and keep the clock/date stationary. The system lock-screen exit still needs testing on the actual Pixel.
- Backups now use schema 5 and accept schemas 1–4. Production still builds the exact committed Gradle version, now 1.3.0 / 1003000.

## 1.2.0

- Added an Apps drawer that follows vertical swipes, settles according to distance/velocity and shares its opening/closing animation with taps and Back. Home stays mounted underneath; pulling down at the top of Apps closes it.
- Delayed search autofocus until the drawer settles. Enter/Search now opens a sole result; ambiguous searches do not launch an app.
- Added a shortcut-only reveal after unlocking into nia, keeping its clock/date fixed and avoiding replay on ordinary app returns. Android's system unlock transition remains outside nia's control.
- Replaced the fixed Home gear with optional left/right corner shortcuts: empty, nia Settings or a selected app with one of twelve outline icons. Both default empty; holding blank Home space opens Settings, which is also available through Apps.
- Added subtle shortcut press feedback, system-respecting haptics and a reduced-motion preference. Drawer/reveal animations follow Android's animation-duration scale, including disabled animations.
- Blocked extra input while drawer transitions settle and guarded rapid duplicate app launches. Missing corner apps recover through app selection.
- Exported settings now use schema 4, including corners and motion; schemas 1–3 and existing saved preferences remain supported.

## 1.1.1

- Production builds now use the exact version name and Android version code committed in `gradle.properties`, matching local builds and About nia.
- Removed patch/minor/major selection, exact-version overrides and automatic version bumps from the production workflow.
- Added validation for matching version names/codes and clearer errors for missing, duplicate, already-released or older project versions. Signing, verification and publication retain their existing flow.

## 1.1.0

- Added a portrait clock/date layout measured from the supplied Pixel 10 Pro lock-screen captures, accounting for the status-bar inset.
- Added bundled Google Sans-derived clock fonts, spaced time and a stronger date, with matching OLED monochrome tones.
- Added a Home setting to return to the previous clock layout. Pixel mode keeps header sizing fixed while Home app-name sizes remain adjustable; landscape uses the compact layout.
- Updated Home previews and backup schema 3; imports still accept schemas 1 and 2.
- Kept exact Pixel weather integration deferred because the system Smartspace clock/weather UI has no supported public launcher interface.

## 1.0.0

- Added a guided four-step first launch with persistent progress, app assignment, appearance choices and Home previews.
- Split Settings into focused pages and added an always-available default-launcher switch action.
- Added app-name initials, camel-case initials and word-prefix search in any order, preserving alphabetical results.
- Added About nia with version, privacy information, project/issue links and third-party notices.
- Simplified production releases with automatic patch/minor/major selection, public publication enabled by default, version-derived Android codes and tags created after build verification.
- Existing completed setups, settings, app assignments and schema-1/schema-2 backups remain supported.

## 0.2.0

- Apps now completes the 3×2 Home icon grid or matches the other text-list entries.
- Replaced the app-list settings label with a gear and added the same control to Home.
- Added a visual color wheel and brightness slider with live light/dark previews, preserving presets and hex input.
- Added optional alphabet scrubbing for app lists, including keyboard and accessibility adjustment.
- Added Small / Default / Large Home text with a Settings preview.
- Preserved existing preferences and backups; new options participate in settings export/import and reset.

## 0.1.0

- Initial nia launcher MVP with five fixed app categories, grid/list layouts, text-only app search, coordinated themes, settings backups and signed APK workflows.
