# nia: MVP requirements

Product specification · 30 October 2026

## 1.1.0 lock-screen alignment

- The default portrait clock/date header follows the supplied Pixel 10 Pro reference with fixed full-screen anchors, spaced time and stronger date typography. Existing assignments and Home shortcut order are preserved.
- Pixel clock layout can be disabled in Home settings to restore the scalable classic header. Pixel mode's clock/date do not move with app text sizes; landscape falls back to compact typography.
- Header fonts are bundled static subsets of the open Google Sans release with original OFL licensing and renamed derivative family metadata.
- Backup exports use schema 3, retaining import compatibility with schemas 1 and 2. Older settings default to the new header and can select the previous layout.
- Exact Pixel weather UI/data integration remains deferred; substitute icons and fabricated weather are outside this change.

## 1.0.0 release scope

- Guided setup has Layout, Apps, Appearance and Default Home stages. It stores progress locally, preserves the step through app selection/restarts, and keeps skip, restore and all-app access available. Existing completed setups are not repeated on upgrade.
- Settings has an overview and focused Home, Appearance, App assignments, App list, Backup & reset and About pages. Changing the default launcher is always available from the overview.
- App search accepts accent/case-insensitive substrings, initials (including camel-case names), and multiple word prefixes in any order. Results stay alphabetical and app assignments retain their explicit launch targets.
- About displays the version, local-data privacy summary, project/issue links and third-party notices. The source-license status is reported accurately; a license has not been selected for nia.
- Production defaults to a public release. A stable patch/minor/major bump or exact override resolves against current Git tags, with a source floor of 1.0.0. Semantic versions determine increasing Android version codes. Tests, lint, signing and verification precede tag creation/publication.
- Setup progress remains internal and is excluded from exported settings. Existing preferences and schema-1/schema-2 backups remain compatible. Theme sharing stays deferred.

## 0.2.0 Home and appearance update

- Apps is the sixth fixed entry: a four-square symbol in the 3×2 grid or a text row matching the five categories.
- Settings uses an outlined gear on Home and in the app-list header.
- Home text size has Small / Default / Large choices and a live preview. It scales the clock, date and list entries alongside Android's font scaling.
- A visual hue/saturation wheel and brightness slider supplement the presets and hex field. Light/dark palette previews update while picking; dragging commits on release and valid hex commits with Apply. Accessible wheel actions and keyboard controls are available.
- Optional alphabet scrubbing uses a 48dp-wide target on the right edge. Letter labels appear while interacting or using keyboard focus; the scrubber hides during search. It defaults off and also works in app-assignment lists.
- Existing saved settings and version-1 backups load with Default text size and the scrubber off. Exports use schema 2 with both new preferences; imports accept schemas 1 and 2.
- Theme-only sharing remains deferred.

**Status:** Implementation specification for nia in [robin-siegl/Nia](https://github.com/robin-siegl/Nia). The MVP includes all core launcher flows and a production build workflow. The implementation decisions below resolve earlier proposed defaults; the selected-color theme replaces the earlier manual foreground/background design.

## MVP implementation decisions

- Product name: **nia**. Package: `dev.robinsiegl.nia`; development builds use `.debug`.
- Fixed order: Phone, Messages, Music, Calendar, Maps. Icon grid has three columns; the text list keeps the same order.
- Appearance: **Black & white** or **Color**, with **System / Light / Dark** behavior. Black & white in dark mode uses exactly `#000000`.
- In Color mode, a single selected seed generates a pastel background/dark foreground pair for light mode and a dark background/pastel foreground pair for dark mode. Selecting a background-family or foreground-family color drives the same matched pair. Six quick choices and a hex input are included. An OLED toggle keeps pure black with colored text in dark mode.
- Generated pairs target at least 7:1 foreground/background contrast. Both variants are previewed together. Manual independent foreground/background editing is superseded by this paired-color design.
- Initial layout is the icon grid; appearance starts with Black & white following System; search autofocus starts disabled.
- Kotlin + Compose, persistent DataStore settings, personal profile only. No network permission or account.
- Compile/target SDK 36, minimum SDK 29. Pixel 10 Pro on the user's reported Android 17 is the primary physical acceptance target; an SDK target level is distinct from the OS versions on which the app can run. Physical validation is still required.
- Backup: strict, versioned personal-profile JSON with validation, preview, atomic application and recovery from unreadable existing settings.
- Production: manually triggered **Production APK** workflow, persistent release key configured once through the four **ANDROID_KEYSTORE_BASE64 / ANDROID_KEYSTORE_PASSWORD / ANDROID_KEY_ALIAS / ANDROID_KEY_PASSWORD** Actions secrets, matching pip-os. Optional GitHub release publication. Missing signing configuration fails with setup instructions.

The detailed planning record follows. Where it describes independently selected background/foreground colors or lists the product name/repository as open, the decisions above supersede it.

## 1. Product goal

Provide a quiet Android home screen for everyday tasks. Five permanent category shortcuts provide quick access to essential apps. A text-only alphabetical app list keeps other apps available while removing their familiar visual branding from the launcher.

The intended benefit is less visual clutter and more deliberate app selection. Success means the phone remains useful and dependable in everyday and unexpected situations. Reduced scrolling is an aspiration to evaluate through use, rather than a promised behavioral outcome.

## 2. Decision status

- **Confirmed:** Explicitly selected by the user in requirements planning.
- **Proposed:** A concrete default or implementation behavior suggested for review. It is not yet a user-confirmed requirement.
- **Open:** Information or a decision still needed.

All behavior described after the confirmed ledger is a proposed elaboration unless it directly restates a confirmed choice. This distinction also applies to the acceptance criteria.

### Confirmed decisions

| ID | Requirement |
| --- | --- |
| C01 | Provide a black appearance with an exactly `#000000` background and off-white text/icons. |
| C02 | Provide a white appearance with dark text/icons. |
| C03 | Also allow a custom background color and one shared foreground color for text and icons. |
| C04 | Home has two selectable presentations: a top-aligned grid of simple category icons, or a text-only list of category names. |
| C05 | The five Home categories are Phone, Messages, Music, Calendar and Maps. Users cannot add categories or rearrange them. |
| C06 | Users configure which app each category opens. |
| C07 | Show time and date above the shortcuts. Tapping time opens Clock; tapping date opens the assigned Calendar app. |
| C08 | Open the full app list by swiping up or tapping a small “Apps” entry on Home. |
| C09 | The app list displays alphabetical app names without app icons, with search available. |
| C10 | Include a setting to enable or disable automatic search focus when the app list opens. |
| C11 | Prioritize the user's Pixel 10 Pro with its standard out-of-the-box setup for the first release. |
| C12 | The first-release interface is English. |
| C13 | Support exporting and importing launcher settings through a file. |
| C14 | Distribute installable APKs for personal use and public GitHub releases. |
| C15 | The target phone uses its normal personal profile; the user does not use a work profile or Private Space. |

## 3. Home screen

### Shared structure

Time and date occupy a compact header. The shortcuts start beneath that header, near the top of the usable screen. A small “Apps” entry follows the shortcut area. Remaining space is intentionally left empty.

The five slots are permanent. Assigning an app changes the launch target while retaining the category's icon, label and position. There are no drag handles, rearrangement gestures or additional Home pages.

**Proposed fixed order:** Phone, Messages, Music, Calendar, Maps. The category set is confirmed; this exact sequence is still a proposal.

### Icon grid

Use simple monochrome vector symbols with a consistent stroke and visual weight. The icons represent categories rather than assigned app brands. Grid mode is icon-only, with category names supplied to accessibility services.

| Category | Symbol | Example target |
| --- | --- | --- |
| Phone | Telephone handset | The user's dialer app |
| Messages | Speech bubble | WhatsApp, Signal or an SMS app |
| Music | Music note | Spotify or a local music player |
| Calendar | Calendar | The user's calendar app |
| Maps | Map or navigation marker | Waze or Google Maps |

Proposed phone layout: three columns in row-major order, with two items on the second row. Do not stretch the final two items into unrelated positions. Exact margins and icon sizes belong in the visual design pass on the target phone.

### Text list

Display the same five categories in the same order as plain text: “Phone”, “Messages”, “Music”, “Calendar” and “Maps”. There are no category icons in this mode. Each entire row is tappable.

Switching presentation changes neither assignments nor order. Custom labels, individual icon replacements and per-item styling are outside the proposed first-release scope.

### Time and date

Proposed formatting follows the device's time zone and 12/24-hour preference. Date formatting follows its regional preference; launcher controls and category names remain English. Display hours and minutes without seconds.

The date uses exactly the same target as the Calendar shortcut. For the time action, resolve a suitable clock/alarms handler. If there is no unambiguous handler, offer an app choice and remember it. Allow changing that Clock assignment in settings. Clock remains a header action, rather than becoming a sixth Home category.

Refresh the header on resume and on relevant time, date, format and time-zone changes. Avoid a continuously running seconds timer.

## 4. Assigning and opening apps

**Proposed behavior:** One launch target per category. Assignments are specific to this launcher and do not change Android's system defaults.

The app selector is a searchable text list. It lets the user choose an accessible launchable app rather than relying on uncertain category classification. The category's purpose is communicated by its fixed label.

| Situation | Proposed behavior |
| --- | --- |
| Category has a valid assignment | Open that app directly. |
| Category has no assignment | Open its app selector. Keep the category in its permanent position. |
| Assigned app is uninstalled or its launch activity disappears | Explain briefly that the app is unavailable and offer reassignment. |
| App is temporarily unavailable | Preserve the assignment and explain the unavailable state. Offer reassignment without forcing it. |
| Launch fails | Keep the launcher usable, show a short error and offer reassignment when appropriate. |
| App is installed, removed, renamed or enabled/disabled | Update the app list without requiring a launcher restart. |

Persist stable launch activity identity in the current personal profile, rather than relying on a display name. Where multiple launchable entries have the same name, add the minimum identifying text needed to choose correctly.

Keep normal launch paths local. Opening Messages does not require reading messages; opening Calendar does not require reading events. The proposed scope does not display those apps' content inside the launcher.

## 5. Full app list and search

### List presentation

Use a vertical, scrollable list of actual installed app names, sorted alphabetically. Names may contain any language, regardless of the launcher's English interface. App icons are absent from the main list, search results and assignment picker.

Provide a search field at the top and a small “Launcher settings” control. Keep the list predictable: proposed first-release scope has no usage-ranked ordering, recommendations, recent-app section, notification badges or promotional content.

For the proposed first release, “all apps” means launchable entries in the phone's current personal profile. Background-only packages are not useful launch targets. Account for multiple launch activities without silently discarding distinct entries. Keep the user's personal app list free of profile tabs or controls that have no purpose on this phone. See the official [LauncherApps documentation](https://developer.android.com/reference/android/content/pm/LauncherApps).

### Search behavior

- Opening the list starts with a blank query and the full alphabetical list.
- With autofocus disabled, opening the list does not summon the keyboard.
- With autofocus enabled, focus the search field and request the keyboard. The full list remains until the user types.
- Proposed default: autofocus is disabled.
- Typing filters by visible app name, case-insensitively. Proposed matching also ignores common accent differences and surrounding whitespace.
- Keep filtered results alphabetical. Search does not use usage history.
- Show “No apps found” when appropriate, with a clear way to clear the query.
- Clearing the query restores the full list. Tapping a result launches it.
- Search is limited to available apps; proposed scope has no web, contacts, file or in-app content search.

## 6. Navigation and state

| Event | Proposed result |
| --- | --- |
| Swipe up within Home content | Open the app list. |
| Tap “Apps” | Open the same app list with the same autofocus preference. |
| Press Home | Show Home, close launcher sub-screens, dismiss the keyboard and clear transient search state. |
| Press Back with the app-list keyboard visible | Dismiss the keyboard first. |
| Press Back in the app list with the keyboard dismissed | Return Home and clear the query. |
| Press Back from settings or an assignment picker | Return to the previous launcher screen. |
| Select an app to launch | Open the target. Prepare the launcher to return to a clean Home screen. |
| Reopen the app list after closing it | Start at the top with a blank query. |
| Press Back while already on Home | Keep the Home surface available. |

System navigation and the notification shade must remain reachable. Launcher swipe handling must not interfere with the phone's system gesture areas. Use safe insets for camera cutouts, status bars, navigation bars and the keyboard.

Proposed settings access is through the app list. Home long presses do not edit shortcuts. An optional long press on an app-list entry may open its Android App info screen; this is a proposed convenience, not a confirmed feature.

## 7. Appearance and accessibility

### Theme model

| Mode | Background | Shared foreground | Status |
| --- | --- | --- | --- |
| OLED | `#000000` | Suggested `#E6E6E6` | Exact background confirmed; foreground value proposed |
| Light | `#FFFFFF` | Suggested `#171717` | White/dark appearance confirmed; exact foreground value proposed |
| Custom | User-selected opaque color | User-selected opaque color | Confirmed capability |

Apply the chosen colors consistently to launcher-owned Home, app-list and settings surfaces. The OLED canvas must remain pure black: avoid automatic surface tinting, gradients or translucent wallpaper effects that turn it gray. System-owned dialogs and the keyboard retain their platform styling.

Proposed custom controls: a simple color picker, editable `#RRGGBB` values, preview and reset. Reject malformed values before applying them. Warn about poor readability, and keep recovery controls readable in the preview. Preserve saved custom colors when switching temporarily to a preset.

Proposed initial appearance is OLED, with icon-grid Home. Those defaults have not been explicitly selected by the user. There is no proposed automatic theme schedule in the first release.

### Usability requirements proposed for implementation

- Respect system font scaling; avoid cutting off app names or losing access to controls.
- Give interactive elements at least 48 dp touch areas, including visually small “Apps” and settings controls. This follows [Android's accessibility guidance](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).
- Give icons meaningful spoken category labels. Expose switches, search and navigation with appropriate accessibility semantics.
- Make every swipe-accessible destination reachable through a visible control too.
- Keep transitions restrained and respect platform animation preferences.
- Keep system status information available and legible against the selected theme.

## 8. Settings

Proposed settings organization:

| Group | Controls |
| --- | --- |
| Home | Icon grid / text list |
| Appearance | OLED / light / custom; custom background and foreground |
| App assignments | Phone, Messages, Music, Calendar, Maps; Clock handler when needed |
| App list | Automatically focus search when opening |
| Backup | Export settings; import settings |
| Launcher | Set as default Home app; app version |
| Reset | Reset appearance/behavior while keeping assignments; separate full reset |

Preferences persist across process recreation, device restart and normal app updates. A change should appear immediately or through an explicit preview/apply interaction, rather than requiring a restart.

## 9. Backup and restore

**Confirmed capability:** Export and import a settings file.

**Proposed format:** A small, versioned UTF-8 JSON file, created and selected through Android's system document picker. The [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files) provides this user-selected file flow.

### Export contents

Include a format identifier, schema version and export timestamp, followed by:

- Home presentation: grid or list.
- Active appearance mode and saved custom background/foreground colors.
- Search autofocus preference.
- The five fixed category assignments, where present.
- Explicit Clock assignment, if one has been saved.

Export launcher configuration only. Exclude search queries, app usage history, other apps' data and a complete installed-app inventory. Fixed Home order is part of the product definition, rather than a configurable exported value.

### Import behavior

1. Let the user select a file. Canceling leaves everything untouched.
2. Validate the format, supported schema version, field types, colors, layout values and target identities before changing settings.
3. Show a short summary of the settings to replace and any targets requiring reassignment.
4. Apply valid settings together, then refresh the interface.
5. If validation or saving fails, retain the previous settings and explain the problem.

Proposed file-size limit: 256 KiB. Reject unsupported newer schemas with a useful message. Migrate older schemas only where a migration has been explicitly implemented.

Restoring does not install missing apps. Valid appearance and behavior settings can be restored while unavailable assignments are left unassigned and clearly listed. The five fixed Home slots remain visible.

Validate restored targets against accessible, enabled launch activities in the current personal profile. Imports cannot introduce extra categories, reordered slots or arbitrary executable intents. The first-release backup format declares its personal-profile scope. Reject imports requesting unsupported profile targets without applying changes; do not reinterpret device-local profile identifiers as personal-profile assignments.

## 10. First launch and default Home integration

Proposed setup is short and skippable:

1. Explain the five fixed categories and the text-only app list.
2. Choose grid/list and a theme, with usable defaults already selected.
3. Assign the five apps; incomplete assignments can be finished later.
4. Offer “Set as default launcher” using Android's system selection flow.

The full app list must be usable before assignments are complete. Record setup completion so routine launches do not repeat onboarding.

The app must register and operate as a real Home app. Android defines the [Home role and its required intent handling](https://developer.android.com/reference/androidx/core/role/RoleManagerCompat), and [RoleManager](https://developer.android.com/reference/android/app/role/RoleManager) provides the user-consent flow. Check available platform capabilities rather than assuming that every device exposes identical handlers.

## 11. Device and profile scope

### Confirmed target

| Detail | Target | Evidence/status |
| --- | --- | --- |
| Phone | Google Pixel 10 Pro | Confirmed by the user |
| Setup | Standard out-of-the-box Pixel setup | Confirmed by the user |
| Profile | Normal personal profile; no work profile or Private Space in use | Confirmed by the user |
| Android baseline | Android 17, API level 37 | User reports the latest Android version and believes it is 17; planned test baseline |
| Navigation | Default Pixel gesture navigation | Working assumption from the standard setup; verify during device testing |

Google announced the [stable Android 17 release](https://android-developers.googleblog.com/2026/06/Android-17.html) on 16 June 2026, and the official [SDK platform table](https://developer.android.com/guide/topics/manifest/uses-sdk-element) maps it to API level 37. This verifies that Android 17 is a released platform, not the version actually installed on the user's phone. Record the installed Android version, build number and active navigation mode before physical-device acceptance testing. There is no need to choose a beta build for this target.

Minimum supported Android version remains an implementation choice; it is distinct from the Android 17 acceptance target. Orientation policy and numerical performance budgets will be chosen in the visual/implementation plan for this phone.

Do not claim tablet, foldable or broad manufacturer compatibility from phone-only testing. The layout should nevertheless accommodate font scaling, keyboard changes and reasonable window resizing without losing access to the app list.

### Proposed first-release profile boundary

Ship the personal-profile experience first. Defer work-profile and Private Space integration, their controls and their acceptance tests. Document that boundary in public APK release notes. The user's confirmed current setup does not require those features.

If profile support is added later, handle each profile's identity and availability explicitly. Private Space would require a dedicated, hideable section with lock/unlock handling, with locked apps excluded from lists and search. Those are [Android's documented launcher requirements for Private Space](https://developer.android.com/about/versions/15/behavior-changes-all#private-space).

Any future profile support must also define Home assignment and backup policies before implementation. Do not advertise those profiles as supported in the personal-profile first release.

## 12. Technical direction and data

**Proposed implementation:** Native Kotlin with Jetpack Compose for the UI, Android launcher/profile APIs for discovery and launching, and DataStore for small persistent settings. Android recommends [Compose for native UI](https://developer.android.com/develop/ui/compose); [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) supports asynchronous, transactional settings storage. Dependency versions and SDK levels should be selected against current stable tooling when implementation begins.

Separate app discovery/launching, persistent preferences, backup validation and UI state so each can be verified independently. A backend is unnecessary for this scope.

Proposed privacy defaults are offline operation, no account, no analytics, no ads and no launcher-maintained usage history. Core features do not depend on contacts, message contents, call logs, calendar events, notification access or an accessibility service. Profile support and package visibility must use the appropriate Android APIs and permissions.

Refresh app availability from platform events and on resume as needed. Load/search the local app-name index without blocking interaction. Target device measurements should guide any numerical performance budget.

## 13. APK delivery and GitHub releases

**Confirmed:** Personal installation and publicly shared GitHub APK releases.

**Proposed delivery setup:**

- A debug build for development and a distinct signed release APK for public installation.
- A manually triggered GitHub Actions build that runs checks and uploads a downloadable APK artifact.
- Separate visible check results for meaningful unit tests, Android lint and the release build.
- Public versioned GitHub releases with the APK, release notes and installation/update instructions.
- A stable application ID, increasing version codes and a protected release signing key.
- A recorded source revision and checksum for each published APK.

Ordinary APK upgrades should retain the signing identity; Android's [app-signing documentation](https://developer.android.com/studio/publish/app-signing) explains certificate checks during updates. Test an actual update over an earlier release and verify that settings survive.

Choose the repository, product name, package ID and source license before the first public release. Public APK distribution leaves the source-license decision open. Google Play submission and an in-app updater are outside the proposed first release.

## 14. Acceptance criteria

These are planned checks, not claims that a build exists or has passed testing. **C** checks trace confirmed requirements; **P** checks trace proposed behavior and become release gates only when that behavior is adopted.

| ID | Basis | Acceptance check |
| --- | --- | --- |
| AC01 | C | In the black preset, unobstructed launcher canvas pixels are exactly `#000000`. Text and icons remain off-white. |
| AC02 | C | Light mode uses a white background and dark text/icons; custom mode applies one background and one shared foreground. |
| AC03 | C | Grid mode shows simple category icons near the top beneath time/date. List mode shows only the five category names in that area. |
| AC04 | C | Users cannot add Home categories or rearrange them. Switching layout preserves the same slots. |
| AC05 | C | Each configured category opens its assigned app. Tapping date uses the Calendar assignment; tapping time opens Clock. |
| AC06 | C | Swipe up and “Apps” both open the same alphabetical, text-only app list with search. |
| AC07 | C | App icons do not appear in the list or search results. Apps remain launchable from both. |
| AC08 | C | The autofocus setting supports both enabled and disabled behavior when opening the list. |
| AC09 | C | The interface is English and is usable on the user's Pixel 10 Pro with its standard personal-profile setup. Record the actual Android version/build in the device test results. |
| AC10 | C | Export a settings file, change settings, then import it and recover the saved configuration. |
| AC11 | C | A signed release APK can be downloaded from the intended GitHub release and installed on the target phone. |
| AC12 | P | Search filters name matches case-insensitively; clearing restores the full alphabetical list. No results has a usable clear action. |
| AC13 | P | Home returns to a clean screen and the next app-list opening starts with an empty query. Back dismisses the keyboard before closing the list. |
| AC14 | P | Installing/removing an app updates the list without restarting the launcher. An unavailable assignment offers recovery without crashing. |
| AC15 | P | On the personal-profile target, the app list and assignment picker show accessible personal apps without irrelevant work/private-profile controls. |
| AC16 | P | Backup declares the supported profile scope. Importing unsupported profile targets leaves current settings unchanged and explains the unsupported scope. |
| AC17 | P | Canceling or importing malformed, oversized or unsupported backup files leaves existing settings unchanged. |
| AC18 | P | A backup containing unavailable targets restores other valid settings and identifies assignments needing attention. |
| AC19 | P | An import cannot add categories, reorder them or install arbitrary launch intents. |
| AC20 | P | Settings survive process recreation, reboot and an update signed with the intended release identity. |
| AC21 | P | Onboarding may be skipped, all apps remain reachable, and the Android default-Home selection works on the target phone. |
| AC22 | P | Large system text, accessibility labels, touch areas, keyboard insets and system navigation are checked on the target phone. |
| AC23 | P | Midnight and time-zone/12–24-hour changes update the header correctly. |
| AC24 | P | Low-contrast previews and invalid color input provide a readable recovery path. |

Use focused automated tests for search ordering, settings serialization/migration, backup rejection and launcher state transitions. Use UI checks for layout modes, focus/keyboard behavior and accessibility semantics. Verify Home-role behavior, launching, gestures, real keyboard behavior and APK upgrades on the physical phone. Device-dependent features should not be declared complete solely from mocked tests.

## 15. Proposed first-release boundary

Deliver the five-category Home, two presentations, time/date actions, themes, app assignments, searchable text list, autofocus setting, default-Home setup, backup/restore and APK delivery.

Keep the initial scope focused: work-profile and Private Space integration, widgets, configurable Home categories, additional Home pages, folders, icon packs, wallpapers, notification feeds, embedded media controls, cloud synchronization and app-blocking timers are deferred. These are scope proposals; the user has explicitly ruled out adding/rearranging Home categories.

## 16. Remaining decisions

| Priority | Open item | Why it matters |
| --- | --- | --- |
| Record during device validation | Installed Android version/build and active navigation mode on the confirmed Pixel 10 Pro | Confirms the physical test environment against the planned Android 17 baseline. |
| Needed for visual design | Exact fixed order, initial grid/list choice and grid geometry | Completes the Home layout without adding customization controls. |
| Needed before publication | Product name, repository, package identity, release-signing setup and source license | Establishes install/update identity and the public release destination. |

The core product scope is sufficiently defined to begin a visual design and implementation plan. The proposed defaults above are reviewable starting points; they do not silently convert into confirmed user decisions.
