# Changelog

## 1.5.0 (unreleased)

- Replace the fading Home header with one clock/date that continuously shrinks as the shortcuts move upward. Simplify the Apps pull to a chevron and one threshold haptic, and fix the large-text list handoff.
- Remove the automatic and sticky Apps entry. Allow up to nine editable shortcuts, each opening an app or running Open Apps, Search apps or Toggle flashlight. Commands are optional on Home and in Quick corners.
- Add Start a timer on Home and in Quick corners. Open a compatible Clock app's timer setup through Android, leaving duration selection, timing, alerts and cancellation to that app. Handle missing apps and duplicate taps without adding a nia timer service.
- Add long-press drag reordering on Home and in Settings → Home shortcuts, with local previews, edge scrolling, cancellation and keyboard/accessibility alternatives.
- Add Add to Home to the redesigned app action menu, opening a prefilled shortcut editor. Preserve App info and Quick corner assignments.
- Export backup schema 7 with validated command targets and continue importing schemas 1–6 without adding a new Apps shortcut. Preserve existing names, icons, targets, order and header assignments.

## 1.4.0

- Replace the hand-drawn shortcut icons with 192 bundled Material Symbols Outlined, covering nine categories. Keep existing icon IDs compatible with saved settings and backups, and use matching Material Apps/Settings icons.
- Add one reusable icon picker for Home apps and Quick corners, with labeled tiles, name/keyword search, categories, selection preview, explicit confirmation, state restoration and a compact layout for large text or the keyboard.
- Rename Corner shortcuts to Quick corners while preserving existing assignments and backup keys. Bundle Google's Apache 2.0 license and verify the pinned catalog/assets in CI.

- Make every Home app editable: add, remove, rename, reassign, choose an outline icon and reorder in Settings → Home apps. Keep up to eight apps plus the reserved Apps entry, with one order shared across grid/list.
- Let new users add apps during setup or skip to an Apps-only Home. Migrate existing five-category setups into ordinary editable entries without repeating completed setup.
- Add a shared resisted pull to open Apps in both layouts, with a progress cue, threshold haptics and deliberate release/cancel behavior. Large lists scroll first and show a compact header and pinned Apps row; grids stay on one screen. A fling alone cannot open Apps.
- Preserve the Home scroll/header position when canceling or closing Apps. Explicit Home requests and new unlocks start from the expanded header; screen lock also clears open-drawer navigation and stale search. Keep search autofocus after settling and respect reduced/disabled animations.
- Separate Clock and Calendar header targets from Home app entries. Preserve shortcut content, header apps and corners when resetting appearance.
- Export backup schema 6 and import schemas 1–6, preserving legacy corner migrations. Restore previews include custom names, icons, order and header assignments; unavailable apps keep recoverable Home entries.
- Request portrait orientation throughout nia on phones (issue #9). Pixel SystemUI/unlock timing and the shared gesture still need physical-phone verification before publication.

## 1.3.3

- Rewrote the README around nia’s everyday experience, screenshots and installation.
- Added current design, architecture and contributor guides, replacing the old MVP requirements and separate lock-screen notes.
- Added repository Android/launcher skills and AGENTS.md instructions, including a required version/code bump and changelog update for every completed change set.
- Refined release, privacy, device-testing and roadmap documentation while preserving attribution and license notices.

## 1.3.2

- Refined Settings labels, explanations and restore terminology. The overview shows current layout, theme, assigned-app count and corner/search choices.
- Kept Back and the page title visible while scrolling, limited content width on wider screens and improved spacing and text hierarchy.
- Made entire switch rows tappable and exposed single-choice selection states to accessibility services. The corner icon picker highlights its current choice.
- Grouped custom color controls together, simplified About and backup actions, and explained what each reset preserves. Both reset actions now ask for confirmation.
- Preserved saved preferences, backup schema, Home layout and unlock behavior. Production builds use the committed 1.3.2 / 1003002 version.

## 1.3.1

- Keep a queued unlock reveal until Home is resumed, focused and unlocked, and mark it complete after the animation finishes.
- Preserve unfinished reveals through focus interruptions and activity recreation, including a slow handoff from the lock screen.
- Check current lock/screen state before acting on delayed broadcasts, and cancel queued reveals when leaving for another app.
- Preserve the stationary header, animation preferences and Home input recovery. The visual Pixel/SystemUI handoff still needs physical-device validation.

## 1.3.0

- Fixed Home remaining unresponsive after unlocking: the shortcut reveal could finish without clearing its parent touch blocker, leaving buttons and swipe-to-Apps disabled until an unrelated refresh. Completion is now observed while input is blocked, including with reduced/disabled animations or animation-scale changes during the reveal.
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
