package dev.robinsiegl.nia.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.robinsiegl.nia.model.*

private val upVector = outlineIcon("Move up", "M6,14L12,8L18,14")
private val downVector = outlineIcon("Move down", "M6,10L12,16L18,10")

@Composable
internal fun HomeAppsSettings(
    preferences: LauncherPreferences, apps: List<AppEntry>, onEdit: (String?) -> Unit,
    onMove: (String, Int) -> Unit,
) {
    SettingsDescription("Choose up to eight apps, with your own names and outline icons. The same order is used in both layouts.",
        Modifier.padding(top = 8.dp, bottom = 16.dp))
    preferences.homeShortcuts.forEachIndexed { index, shortcut ->
        Row(Modifier.fillMaxWidth().testTag("home-editor-row-${shortcut.id}"), verticalAlignment = Alignment.CenterVertically) {
            SettingsAction(shortcut.name, apps.find { it.target == shortcut.app }?.label ?:
                if (shortcut.app == null) "Choose an app" else "App unavailable", { onEdit(shortcut.id) },
                modifier = Modifier.weight(1f), leading = { ShortcutGlyph(shortcut.icon, Modifier.size(24.dp)) })
            Column {
                IconButton(onClick = { onMove(shortcut.id, -1) }, enabled = index > 0, modifier = Modifier.size(48.dp)) {
                    Icon(upVector, "Move ${shortcut.name} up")
                }
                IconButton(onClick = { onMove(shortcut.id, 1) }, enabled = index < preferences.homeShortcuts.lastIndex,
                    modifier = Modifier.size(48.dp)) { Icon(downVector, "Move ${shortcut.name} down") }
            }
        }
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
        AppsIcon(Modifier.size(24.dp))
        Column(Modifier.padding(start = 16.dp)) {
            Text("Apps", style = MaterialTheme.typography.bodyLarge)
            SettingsDescription("Always available · stays last")
        }
    }
    OutlinedButton(onClick = { onEdit(null) }, enabled = preferences.homeShortcuts.size < MAX_HOME_APPS,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 12.dp)) { Text("Add app") }
    SettingsDescription("${preferences.homeShortcuts.size} of $MAX_HOME_APPS apps · Apps always stays available",
        Modifier.padding(top = 12.dp))
}

@Composable
internal fun HomeShortcutEditorScreen(
    draft: HomeShortcutDraft, apps: List<AppEntry>, saving: Boolean,
    onChange: ((HomeShortcut) -> HomeShortcut) -> Unit, onChooseApp: () -> Unit,
    onSave: () -> Unit, onRemove: () -> Unit, onBack: () -> Unit,
) {
    var choosingIcon by rememberSaveable { mutableStateOf(false) }
    var removing by rememberSaveable { mutableStateOf(false) }
    BackHandler { if (!saving) onBack() }
    val shortcut = draft.shortcut
    SettingsLayout(if (draft.isNew) "Add Home app" else "Edit Home app", { if (!saving) onBack() }) {
        SectionLabel("App")
        SettingsAction("Opens", apps.find { it.target == shortcut.app }?.label ?:
            if (shortcut.app == null) "Choose an app" else "App unavailable", onChooseApp, enabled = !saving)
        SectionLabel("On Home")
        OutlinedTextField(value = shortcut.name, onValueChange = { value ->
            if (value.length <= MAX_HOME_NAME_LENGTH && value.none { it.isISOControl() }) onChange { it.copy(name = value) }
        }, label = { Text("Name") }, singleLine = true, enabled = !saving,
            modifier = Modifier.fillMaxWidth().testTag("home-shortcut-name"))
        SettingsAction("Icon", shortcut.icon.label, { choosingIcon = true }, enabled = !saving,
            leading = { ShortcutGlyph(shortcut.icon, Modifier.size(24.dp)) })
        SettingsDescription("Changing the app keeps your chosen name and icon. Names apply to Home only.")
        Button(onClick = onSave, enabled = draft.canSave && !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 24.dp)) { Text("Save") }
        if (!draft.isNew) TextButton(onClick = { removing = true }, enabled = !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Remove from Home") }
    }
    if (choosingIcon) ShortcutIconPicker("Home app icon", shortcut.icon, { choosingIcon = false }) { icon ->
        onChange { it.copy(icon = icon) }; choosingIcon = false
    }
    if (removing) AlertDialog(onDismissRequest = { removing = false }, title = { Text("Remove ${shortcut.name}?") },
        text = { Text("The app stays available in Apps. You can add it to Home again.") },
        confirmButton = { TextButton(onClick = { removing = false; onRemove() }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { removing = false }) { Text("Cancel") } })
}
