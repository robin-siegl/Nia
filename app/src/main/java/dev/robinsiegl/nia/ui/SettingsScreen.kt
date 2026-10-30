package dev.robinsiegl.nia.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.robinsiegl.nia.model.*

@Composable
fun SettingsScreen(
    isDefault: Boolean, storageError: Boolean, onBack: () -> Unit,
    onSection: (SettingsSection) -> Unit, onSetDefault: () -> Unit, onChangeDefault: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
        ScreenHeader("Settings", onBack)
        if (storageError) {
            Text("Saved settings could not be read. Restore a backup or reset nia to recover.", fontSize = 14.sp)
            TextButton(onClick = { onSection(SettingsSection.BACKUP) }) { Text("Restore or reset settings") }
        }
        SettingsSection.entries.forEach { section ->
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { onSection(section) }.padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(section.title, fontSize = 20.sp)
                    Text(section.description, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                }
                Text("›", fontSize = 28.sp)
            }
        }
        SectionLabel("DEFAULT LAUNCHER")
        Text(if (isDefault) "nia is your default Home app." else "nia is not your default Home app.", fontSize = 14.sp)
        if (!isDefault) OutlinedButton(onClick = onSetDefault, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text("Set nia as default launcher")
        }
        TextButton(onClick = onChangeDefault, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Change default launcher")
        }
    }
}

@Composable
fun SettingsDetailScreen(
    section: SettingsSection, preferences: LauncherPreferences, apps: List<AppEntry>, storageError: Boolean,
    onBack: () -> Unit, onChange: ((LauncherPreferences) -> LauncherPreferences) -> Unit,
    onAssign: (HomeCategory?) -> Unit, onClear: (HomeCategory?) -> Unit,
    onExport: () -> Unit, onImport: () -> Unit, onResetAppearance: () -> Unit, onResetAll: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
        ScreenHeader(section.title, onBack)
        when (section) {
            SettingsSection.HOME -> HomeSettings(preferences, onChange)
            SettingsSection.APPEARANCE -> AppearanceSettings(preferences, onChange)
            SettingsSection.ASSIGNMENTS -> AssignmentsSettings(preferences, apps, onAssign, onClear)
            SettingsSection.APP_LIST -> {
                SettingSwitch("Focus search automatically", "Show the keyboard when opening Apps.", preferences.searchAutofocus) {
                    value -> onChange { it.copy(searchAutofocus = value) }
                }
                SettingSwitch("Alphabet scrubber", "Drag along the right edge to jump to a letter.", preferences.alphabetScrubber) {
                    value -> onChange { it.copy(alphabetScrubber = value) }
                }
                Text("Search by name, initials, or words in any order. Matching apps stay alphabetical.",
                    fontSize = 14.sp, modifier = Modifier.padding(top = 20.dp))
            }
            SettingsSection.BACKUP -> BackupSettings(storageError, onExport, onImport, onResetAppearance, onResetAll)
            SettingsSection.ABOUT -> AboutContent(onOpenLink)
        }
    }
}

@Composable
internal fun HomeSettings(preferences: LauncherPreferences, onChange: ((LauncherPreferences) -> LauncherPreferences) -> Unit) {
    ChoiceRow(listOf(HomeLayout.GRID to "Icon grid", HomeLayout.LIST to "Text list"), preferences.layout,
        { value -> onChange { it.copy(layout = value) } })
    Spacer(Modifier.height(16.dp))
    SettingSwitch("Pixel clock layout", "Keep the clock and date aligned when you unlock.", preferences.pixelClockLayout) {
        value -> onChange { it.copy(pixelClockLayout = value) }
    }
    if (!preferences.pixelClockLayout || preferences.layout == HomeLayout.LIST) {
        Text(if (preferences.pixelClockLayout) "App text size" else "Home text size", fontSize = 16.sp,
            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
        ChoiceRow(listOf(HomeTextSize.SMALL to "Small", HomeTextSize.DEFAULT to "Default", HomeTextSize.LARGE to "Large"),
            preferences.homeTextSize, { value -> onChange { it.copy(homeTextSize = value) } })
        Text(if (preferences.pixelClockLayout) "Applies to app names. The clock and date keep their lock-screen sizing."
            else "Applies to the clock, date and Home list.", fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
    }
    HomePreview(preferences, Modifier.padding(top = 20.dp))
}

@Composable
internal fun AppearanceSettings(preferences: LauncherPreferences, onChange: ((LauncherPreferences) -> LauncherPreferences) -> Unit) {
    ChoiceRow(listOf(ColorStyle.MONOCHROME to "Black & white", ColorStyle.COLOR to "Color"), preferences.colorStyle,
        { value -> onChange { it.copy(colorStyle = value) } })
    Spacer(Modifier.height(16.dp))
    ChoiceRow(listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark"),
        preferences.themeMode, { value -> onChange { it.copy(themeMode = value) } })
    Text("System follows your phone’s light or dark appearance.", fontSize = 12.sp,
        modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
    if (preferences.colorStyle == ColorStyle.COLOR) ColorSettings(preferences, onChange)
}

@Composable
internal fun AssignmentsSettings(
    preferences: LauncherPreferences, apps: List<AppEntry>, onAssign: (HomeCategory?) -> Unit, onClear: (HomeCategory?) -> Unit,
) {
    HomeCategory.entries.forEach { category ->
        AssignmentRow(category.label, preferences.assignments[category], apps, { onAssign(category) }, { onClear(category) })
    }
    AssignmentRow("Clock", preferences.clock, apps, { onAssign(null) }, { onClear(null) }, "System clock")
}

@Composable
private fun ColumnScope.BackupSettings(storageError: Boolean, onExport: () -> Unit, onImport: () -> Unit,
    onResetAppearance: () -> Unit, onResetAll: () -> Unit) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    Text("A settings backup includes your app assignments, layout, colors and app-list preferences.", fontSize = 14.sp)
    TextButton(onClick = onExport, enabled = !storageError, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Export settings") }
    TextButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("Import settings") }
    SectionLabel("RESET")
    TextButton(onClick = onResetAppearance, enabled = !storageError, modifier = Modifier.fillMaxWidth()) {
        Text("Reset appearance and behavior")
    }
    Text("Keeps your app assignments.", fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    TextButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Reset all settings") }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("Reset nia?") },
        text = { Text("This clears your preferences and app assignments. You can export a backup first.") },
        confirmButton = { TextButton(onClick = { confirmReset = false; onResetAll() }) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } })
}

@Composable
private fun AssignmentRow(label: String, target: AppTarget?, apps: List<AppEntry>, onSelect: () -> Unit,
    onClear: () -> Unit, defaultLabel: String = "Choose an app") {
    val assigned = target?.let { selected -> apps.find { it.target == selected }?.label ?: "Unavailable · choose another" } ?: defaultLabel
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).clickable(role = Role.Button, onClick = onSelect).padding(vertical = 14.dp)) {
            Text(label, fontSize = 17.sp)
            Text(assigned, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
        }
        if (target != null) TextButton(onClick = onClear,
            modifier = Modifier.semantics { contentDescription = "Clear $label assignment" }) { Text("Clear", fontSize = 12.sp) }
    }
}

@Composable
internal fun SettingSwitch(label: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, fontSize = 16.sp)
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
        }
        Switch(checked, onCheckedChange = onChange, modifier = Modifier.semantics { contentDescription = label })
    }
}
