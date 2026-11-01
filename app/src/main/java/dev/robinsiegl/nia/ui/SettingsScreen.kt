package dev.robinsiegl.nia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.robinsiegl.nia.BuildConfig
import dev.robinsiegl.nia.model.*

@Composable
fun SettingsScreen(
    isDefault: Boolean, storageError: Boolean, onBack: () -> Unit,
    onSection: (SettingsSection) -> Unit, onSetDefault: () -> Unit, onChangeDefault: () -> Unit,
    preferences: LauncherPreferences = LauncherPreferences(),
) {
    SettingsLayout("Settings", onBack) {
        if (storageError) {
            SettingsDescription("Your saved settings couldn’t be read. Restore a backup or reset nia to continue.")
            TextButton(onClick = { onSection(SettingsSection.BACKUP) }) { Text("Restore or reset settings") }
        }
        SectionLabel("Personalize")
        listOf(SettingsSection.HOME, SettingsSection.APPEARANCE, SettingsSection.ASSIGNMENTS,
            SettingsSection.SHORTCUTS, SettingsSection.APP_LIST).forEach { section ->
            SettingsAction(section.title, settingsSummary(section, preferences), { onSection(section) })
        }
        SectionLabel("More")
        SettingsAction("Default launcher", if (isDefault) "nia" else "nia is not the default", onChangeDefault)
        if (!isDefault) OutlinedButton(onClick = onSetDefault,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).heightIn(min = 48.dp)) {
            Text("Set nia as default")
        }
        SettingsAction(SettingsSection.BACKUP.title, SettingsSection.BACKUP.description, { onSection(SettingsSection.BACKUP) })
        SettingsAction(SettingsSection.ABOUT.title, "Version ${BuildConfig.VERSION_NAME} · Updates and privacy", { onSection(SettingsSection.ABOUT) })
    }
}

private fun settingsSummary(section: SettingsSection, p: LauncherPreferences): String = when (section) {
    SettingsSection.HOME -> "${if (p.layout == HomeLayout.GRID) "Icon grid" else "Text list"} · ${if (p.pixelClockLayout) "Pixel-style clock" else "Classic clock"}"
    SettingsSection.APPEARANCE -> "${if (p.colorStyle == ColorStyle.MONOCHROME) "Black & white" else "Color"} · ${when (p.themeMode) {
        ThemeMode.SYSTEM -> "Follow system"; ThemeMode.LIGHT -> "Light"; ThemeMode.DARK -> "Dark"
    }}"
    SettingsSection.ASSIGNMENTS -> "${p.homeShortcuts.size} ${if (p.homeShortcuts.size == 1) "app" else "apps"} · Add, edit and arrange"
    SettingsSection.SHORTCUTS -> when {
        p.leftShortcut.action == ShortcutAction.APP && p.rightShortcut.action == ShortcutAction.APP -> "Both corners"
        p.leftShortcut.action == ShortcutAction.APP -> "Left corner"
        p.rightShortcut.action == ShortcutAction.APP -> "Right corner"
        else -> "No shortcuts"
    }
    SettingsSection.APP_LIST -> if (p.searchAutofocus) "Keyboard opens with Apps" else "Tap search to open the keyboard"
    else -> section.description
}

@Composable
fun SettingsDetailScreen(
    section: SettingsSection, preferences: LauncherPreferences, apps: List<AppEntry>, storageError: Boolean,
    onBack: () -> Unit, onChange: ((LauncherPreferences) -> LauncherPreferences) -> Unit,
    onAssign: (HeaderTarget) -> Unit, onClear: (HeaderTarget) -> Unit,
    onExport: () -> Unit, onImport: () -> Unit, onResetAppearance: () -> Unit, onResetAll: () -> Unit,
    onOpenLink: (String) -> Unit,
    onAssignCorner: (CornerPosition) -> Unit = {},
    onEditHome: (String?) -> Unit = {}, onMoveHome: (String, Int) -> Unit = { _, _ -> },
    updateCheck: UpdateCheck = UpdateCheck.Idle, onCheckUpdates: () -> Unit = {},
) {
    SettingsLayout(section.title, onBack) {
        when (section) {
            SettingsSection.HOME -> {
                HomeSettings(preferences, onChange)
                SectionLabel("Clock & date")
                AssignmentsSettings(preferences, apps, onAssign, onClear)
            }
            SettingsSection.SHORTCUTS -> QuickCornerSettings(preferences, apps, onChange, onAssignCorner)
            SettingsSection.APPEARANCE -> AppearanceSettings(preferences, onChange)
            SettingsSection.ASSIGNMENTS -> HomeAppsSettings(preferences, apps, onEditHome, onMoveHome)
            SettingsSection.APP_LIST -> {
                SettingSwitch("Keyboard on open", "Open the keyboard when Apps is ready to search.", preferences.searchAutofocus) {
                    value -> onChange { it.copy(searchAutofocus = value) }
                }
                SettingSwitch("A–Z navigation", "Drag along the right edge to jump to a letter.", preferences.alphabetScrubber) {
                    value -> onChange { it.copy(alphabetScrubber = value) }
                }
                SettingsDescription("Search by name, initials or words in any order. Results stay alphabetical.",
                    Modifier.padding(top = 24.dp))
            }
            SettingsSection.BACKUP -> BackupSettings(storageError, onExport, onImport, onResetAppearance, onResetAll)
            SettingsSection.ABOUT -> AboutContent(onOpenLink, updateCheck, onCheckUpdates)
        }
    }
}

@Composable
internal fun HomeSettings(preferences: LauncherPreferences, onChange: ((LauncherPreferences) -> LauncherPreferences) -> Unit) {
    SectionLabel("Layout")
    ChoiceRow(listOf(HomeLayout.GRID to "Icon grid", HomeLayout.LIST to "Text list"), preferences.layout,
        { value -> onChange { it.copy(layout = value) } })
    Spacer(Modifier.height(16.dp))
    SettingSwitch("Pixel-style clock", "Sized and positioned for the Pixel 10 Pro lock screen.", preferences.pixelClockLayout) {
        value -> onChange { it.copy(pixelClockLayout = value) }
    }
    if (!preferences.pixelClockLayout || preferences.layout == HomeLayout.LIST) {
        SectionLabel(if (preferences.pixelClockLayout) "App text size" else "Home text size")
        ChoiceRow(listOf(HomeTextSize.SMALL to "Small", HomeTextSize.DEFAULT to "Default", HomeTextSize.LARGE to "Large"),
            preferences.homeTextSize, { value -> onChange { it.copy(homeTextSize = value) } })
        SettingsDescription(if (preferences.pixelClockLayout) "Applies to app names. The clock and date keep their size."
            else "Applies to the clock, date and app names.", Modifier.padding(top = 12.dp))
    }
    SectionLabel("Preview")
    HomePreview(preferences)
}

@Composable
internal fun AppearanceSettings(preferences: LauncherPreferences, onChange: ((LauncherPreferences) -> LauncherPreferences) -> Unit) {
    SectionLabel("Colors")
    ChoiceRow(listOf(ColorStyle.MONOCHROME to "Black & white", ColorStyle.COLOR to "Color"), preferences.colorStyle,
        { value -> onChange { it.copy(colorStyle = value) } })
    SectionLabel("Theme")
    ChoiceRow(listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark"),
        preferences.themeMode, { value -> onChange { it.copy(themeMode = value) } })
    SettingsDescription("System follows your phone’s light or dark theme.", Modifier.padding(top = 12.dp))
    if (preferences.colorStyle == ColorStyle.COLOR) {
        SectionLabel("Theme color")
        ColorSettings(preferences, onChange)
    }
    SectionLabel("Animations")
    ChoiceRow(listOf(MotionMode.SYSTEM to "Standard", MotionMode.REDUCED to "Reduced"), preferences.motionMode,
        { value -> onChange { it.copy(motionMode = value) } })
    SettingsDescription("Reduced uses short fades with less movement. Both options follow Android’s animation settings.",
        Modifier.padding(top = 12.dp))
}

@Composable
internal fun AssignmentsSettings(
    preferences: LauncherPreferences, apps: List<AppEntry>, onAssign: (HeaderTarget) -> Unit, onClear: (HeaderTarget) -> Unit,
) {
    SettingsDescription("These choices stay separate from your Home apps.", Modifier.padding(top = 8.dp, bottom = 12.dp))
    AssignmentRow("Clock", preferences.clock, apps, { onAssign(HeaderTarget.CLOCK) }, { onClear(HeaderTarget.CLOCK) }, "System clock")
    AssignmentRow("Calendar", preferences.calendar, apps, { onAssign(HeaderTarget.CALENDAR) }, { onClear(HeaderTarget.CALENDAR) }, "Choose an app")
}

private enum class ResetOption { APPEARANCE, ALL }

@Composable
private fun BackupSettings(storageError: Boolean, onExport: () -> Unit, onImport: () -> Unit,
    onResetAppearance: () -> Unit, onResetAll: () -> Unit) {
    var reset by rememberSaveable { mutableStateOf<ResetOption?>(null) }
    SectionLabel("Backup")
    SettingsDescription("Save your layout, colors, app choices and other preferences in a settings file.")
    SettingsAction("Save backup", "Choose where to save the file.", onExport, enabled = !storageError)
    SettingsAction("Restore backup", "Review changes before applying them.", onImport)
    SectionLabel("Reset")
    SettingsAction("Reset appearance & behavior", "Restore defaults and keep all app assignments and quick corners.",
        { reset = ResetOption.APPEARANCE }, enabled = !storageError, showChevron = false)
    SettingsAction("Reset all settings", "Clear preferences and app assignments, then start setup again.",
        { reset = ResetOption.ALL }, showChevron = false)
    reset?.let { kind ->
        val all = kind == ResetOption.ALL
        AlertDialog(onDismissRequest = { reset = null },
            title = { Text(if (all) "Reset all settings?" else "Reset appearance & behavior?") },
            text = { Text(if (all) "Your preferences and app assignments will be cleared. You can save a backup before resetting."
                else "Your layout, colors, text size, search and animation settings will return to their defaults. App assignments and quick corners will stay as they are.") },
            confirmButton = { TextButton(onClick = { reset = null; if (all) onResetAll() else onResetAppearance() }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { reset = null }) { Text("Cancel") } })
    }
}

@Composable
private fun AssignmentRow(label: String, target: AppTarget?, apps: List<AppEntry>, onSelect: () -> Unit,
    onClear: () -> Unit, defaultLabel: String = "Not assigned") {
    val assigned = target?.let { selected -> apps.find { it.target == selected }?.label ?: "App unavailable" } ?: defaultLabel
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SettingsAction(label, assigned, onSelect, modifier = Modifier.weight(1f))
        if (target != null) TextButton(onClick = onClear,
            modifier = Modifier.semantics { contentDescription = "Clear $label assignment" }) { Text("Clear") }
    }
}

@Composable
internal fun SettingSwitch(label: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
        .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            SettingsDescription(description, Modifier.padding(top = 4.dp))
        }
        Switch(checked, onCheckedChange = null)
    }
}
