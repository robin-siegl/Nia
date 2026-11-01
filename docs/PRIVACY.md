# Privacy

nia's Home, app list, search, settings and backups work offline. There are no accounts, ads, analytics or crash-reporting services. The only in-app network request is the GitHub update check you start in **Settings → About nia**.

## Installed apps

nia reads launchable app names and their launch targets from Android to list, search and open your apps. This happens on your device. nia does not upload the app list or keep an app-usage history.

## Settings and setup

Your chosen apps and commands, layout, colors, app-list preferences and unfinished setup step are kept in nia's private app storage. nia does not read messages, contacts, calendar events, location or notification contents.

## Local commands

Open Apps and Search apps use nia's local app list. Toggle flashlight uses Android's torch control and observes its current on/off state. It does not open the camera, capture images, request camera permission or send torch state elsewhere. Android and other apps can change or make the flashlight unavailable.

Start a timer opens a compatible Clock app through Android. That app handles the duration you enter, countdown, alerts and cancellation under its own settings. nia does not run a timer in the background or record timer history. Android's normal Set alarm permission allows this handoff; nia does not request exact-alarm access or a runtime permission prompt for it.

## Backups

**Save backup** creates a JSON file wherever you choose through Android's document picker. It contains nia's preferences and assigned app identifiers. It does not contain other apps' data, a complete app inventory, search history or setup progress. If you save to a cloud document provider, that provider handles the file under its own policies.

**Restore backup** reads only the file you select. nia previews each setting and assignment that will change, including unavailable apps. Cancel leaves your settings unchanged. If settings or available apps change before you restore, nia asks you to review the updated result.

Android's automatic app-data backup and transfer are disabled. Use nia's explicit backup actions to move settings between installations.

## Manual update checks

**Check for updates** sends an unauthenticated HTTPS request to `api.github.com/repos/robin-siegl/Nia/releases/latest`. GitHub receives normal connection information, including your IP address and the generic `nia-Android` client header. nia does not send your app list, assignments, settings, device identifier or search history.

Checks never run automatically. Results stay in memory for the current process. Download and release links open in another app, usually your browser; Android handles installation if you choose it. nia does not request package-installation permission or download APKs itself.

## External links

About nia links to GitHub, issue reporting, privacy information and third-party notices. These links open externally. Information you choose to submit to GitHub is handled by GitHub.

## Remove your data

Use **Settings → Backup & reset → Reset all settings**, clear nia's app storage in Android Settings, or uninstall nia to remove locally saved settings. Backup files are separate; delete them from the location where you saved them.
