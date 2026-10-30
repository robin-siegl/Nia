# nia privacy

nia's launcher, search, preferences and backups work offline. It has no accounts, advertising, analytics or crash-reporting service. Internet permission is used only when you manually check GitHub for an update in About nia.

## Installed apps

nia reads launchable app names and launch targets from Android to display the app list, search it and open the apps you choose. This processing happens on your device. nia does not upload your app list or record app-usage history.

## Settings and setup

Your app assignments, layout, appearance, app-list options and unfinished setup step are stored in nia's private app storage. nia does not read messages, contacts, calendar events, location or notification contents.

## Backups

Exporting creates a JSON file at the location you select through Android's document picker. It includes appearance, layout, app-list preferences and assigned app identifiers. It does not include installed-app inventories, search history or setup progress. If you choose a cloud document provider, that provider handles the file under its own policies.

Importing reads only the file you select. Android's automatic app-data backup and transfer are disabled; use nia's explicit settings export/import when moving settings.

Before restoring, nia shows the current and backup values for every setting/app assignment that will change. Unavailable assignments are identified and cleared in the preview. Cancel leaves preferences unchanged. App availability is checked again when applying; a changed result requires reviewing the new preview.

## Manual update checks

Settings → About nia → Check for updates makes an HTTPS request to `api.github.com/repos/robin-siegl/Nia/releases/latest`. GitHub receives the normal connection metadata, including your IP address and the generic `nia-Android` client header. nia does not send your installed apps, app assignments, preferences, device identifier or search history. No account or access token is used.

There are no automatic checks, background update jobs or automatic APK downloads. Results remain in memory for the current nia process. The download/release buttons open GitHub in another app, usually your browser; Android handles any installation you choose. nia does not request permission to install packages.

## External links

About nia offers links to GitHub, issue reporting and license notices. Opening a link uses another app, usually your browser. Any information you submit to GitHub is handled by GitHub.

## Removing data

Use Settings → Backup & reset → Reset all settings, clear nia's app storage in Android Settings, or uninstall nia to remove its locally stored settings. Exported files are separate and must be removed from the location where you saved them.
