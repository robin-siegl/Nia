# Changelog

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
