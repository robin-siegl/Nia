package dev.robinsiegl.nia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.robinsiegl.nia.BuildConfig
import dev.robinsiegl.nia.model.*
import dev.robinsiegl.nia.theme.Palette

@Composable
fun SettingsScreen(
    preferences: LauncherPreferences, apps: List<AppEntry>, isDefault: Boolean, storageError: Boolean,
    onBack: () -> Unit, onChange: ((LauncherPreferences) -> LauncherPreferences) -> Unit,
    onAssign: (HomeCategory?) -> Unit, onClear: (HomeCategory?) -> Unit, onSetDefault: () -> Unit,
    onExport: () -> Unit, onImport: () -> Unit, onResetAppearance: () -> Unit, onResetAll: () -> Unit,
) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
        ScreenHeader("Settings", onBack)
        if (storageError) {
            Text("Saved settings could not be read. Restore a backup or reset nia to recover.", fontSize = 14.sp)
        }
        SectionLabel("HOME")
        ChoiceRow(listOf(HomeLayout.GRID to "Icon grid", HomeLayout.LIST to "Text list"), preferences.layout,
            { value -> onChange { it.copy(layout = value) } })
        SectionLabel("APPEARANCE")
        ChoiceRow(listOf(ColorStyle.MONOCHROME to "Black & white", ColorStyle.COLOR to "Color"), preferences.colorStyle,
            { value -> onChange { it.copy(colorStyle = value) } })
        Spacer(Modifier.height(16.dp))
        ChoiceRow(listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark"),
            preferences.themeMode, { value -> onChange { it.copy(themeMode = value) } })
        Text("System follows your phone’s light or dark appearance.", fontSize = 12.sp,
            modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
        if (preferences.colorStyle == ColorStyle.COLOR) {
            ColorSettings(preferences, onChange)
        }
        SectionLabel("APP ASSIGNMENTS")
        HomeCategory.entries.forEach { category ->
            AssignmentRow(category.label, preferences.assignments[category], apps,
                { onAssign(category) }, { onClear(category) })
        }
        AssignmentRow("Clock", preferences.clock, apps, { onAssign(null) }, { onClear(null) }, defaultLabel = "System clock")
        SectionLabel("APP LIST")
        SettingSwitch("Focus search automatically", "Show the keyboard when opening Apps.", preferences.searchAutofocus) {
            value -> onChange { it.copy(searchAutofocus = value) }
        }
        SectionLabel("BACKUP")
        TextButton(onClick = onExport, enabled = !storageError, modifier = Modifier.fillMaxWidth()) { Text("Export settings") }
        TextButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("Import settings") }
        SectionLabel("LAUNCHER")
        if (isDefault) Text("nia is your default Home app.", fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp))
        else OutlinedButton(onClick = onSetDefault, modifier = Modifier.fillMaxWidth()) { Text("Set nia as default launcher") }
        TextButton(onClick = onResetAppearance, enabled = !storageError, modifier = Modifier.fillMaxWidth()) {
            Text("Reset appearance and behavior")
        }
        Text("Keeps your app assignments.", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), modifier = Modifier.align(Alignment.CenterHorizontally))
        TextButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Reset all settings") }
        Text("nia · ${BuildConfig.VERSION_NAME}", fontSize = 12.sp,
            modifier = Modifier.padding(top = 28.dp).align(Alignment.CenterHorizontally))
    }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("Reset nia?") },
        text = { Text("This clears your preferences and app assignments. You can export a backup first.") },
        confirmButton = { TextButton(onClick = { confirmReset = false; onResetAll() }) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } })
}

@Composable
private fun AssignmentRow(
    label: String, target: AppTarget?, apps: List<AppEntry>, onSelect: () -> Unit, onClear: () -> Unit,
    defaultLabel: String = "Choose an app",
) {
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
private fun SettingSwitch(label: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, fontSize = 16.sp)
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
        }
        Switch(checked, onCheckedChange = onChange, modifier = Modifier.semantics { contentDescription = label })
    }
}

@Composable
private fun ColorSettings(preferences: LauncherPreferences, onChange: ((LauncherPreferences) -> LauncherPreferences) -> Unit) {
    var input by rememberSaveable(preferences.seedColor) { mutableStateOf(preferences.seedColor) }
    val valid = PreferencesValidation.isColor(input)
    Text("Pick one color. nia finds the matching background and text.", fontSize = 14.sp,
        modifier = Modifier.padding(top = 24.dp, bottom = 16.dp))
    val seeds = listOf("Violet" to "#7C3AED", "Rose" to "#D54778", "Blue" to "#2563EB", "Mint" to "#008B75", "Amber" to "#BD7900", "Coral" to "#D8553D")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        seeds.forEach { (name, hex) ->
            val active = preferences.seedColor.equals(hex, ignoreCase = true)
            Box(Modifier.size(48.dp).clickable(role = Role.Button) { onChange { it.copy(seedColor = hex) } }
                .semantics { contentDescription = "$name color" }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(30.dp).border(if (active) 2.dp else 0.dp,
                    if (active) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape).padding(4.dp)
                    .background(Color((0xFF000000L or hex.drop(1).toLong(16)).toInt()), CircleShape))
            }
        }
    }
    Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(input, { input = it.take(7) }, label = { Text("Your color") }, singleLine = true,
            isError = input.isNotEmpty() && !valid, modifier = Modifier.weight(1f),
            supportingText = { if (!valid) Text("Use #RRGGBB") })
        TextButton(onClick = { onChange { it.copy(seedColor = input.uppercase()) } }, enabled = valid,
            modifier = Modifier.padding(start = 8.dp)) { Text("Apply") }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark").forEach { (mode, label) ->
            val colors = Palette.generate(preferences.copy(themeMode = mode), false)
            Column(Modifier.weight(1f).background(Color(colors.background), RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f), RoundedCornerShape(8.dp)).padding(16.dp)) {
                Text("Aa", fontSize = 28.sp, fontWeight = FontWeight.Light, color = Color(colors.foreground))
                Text(label, fontSize = 12.sp, color = Color(colors.foreground))
            }
        }
    }
    SettingSwitch("Pure black in dark mode", "Keeps your colored text on an OLED-black background.", preferences.pureBlackColorBackground) {
        value -> onChange { it.copy(pureBlackColorBackground = value) }
    }
}
