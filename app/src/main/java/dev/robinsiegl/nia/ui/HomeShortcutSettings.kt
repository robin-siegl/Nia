package dev.robinsiegl.nia.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.robinsiegl.nia.R
import dev.robinsiegl.nia.model.*

private val upVector = outlineIcon("Move up", "M6,14L12,8L18,14")
private val downVector = outlineIcon("Move down", "M6,10L12,16L18,10")

@Composable
internal fun HomeAppsSettings(
    preferences: LauncherPreferences, apps: List<AppEntry>, onEdit: (String?) -> Unit,
    onMove: (String, Int) -> Unit,
    onReorder: (suspend (List<String>) -> Boolean)? = null, scroll: ScrollState? = null,
) {
    val reorder = rememberShortcutReorder(preferences.homeShortcuts, onReorder,
        autoScroll = scroll?.let { state -> { delta -> state.scrollBy(delta) } })
    val entries = remember(preferences.homeShortcuts, reorder.preview) { reorder.ordered(preferences.homeShortcuts) }
    var arranging by remember { mutableStateOf<String?>(null) }
    SettingsDescription("Choose up to nine apps or commands. Touch and hold a shortcut to drag it into place. Both Home layouts use the same order.",
        Modifier.padding(top = 8.dp, bottom = 16.dp))
    LookaheadScope {
        val lookahead = this
        Column(Modifier.fillMaxWidth().reorderViewport(reorder)) {
            entries.forEachIndexed { index, shortcut -> key(shortcut.id) {
                Box(Modifier.fillMaxWidth().reorderSlot(shortcut.id, reorder)) {
                    Row(Modifier.fillMaxWidth().reorderShortcut(shortcut, reorder, lookahead, onReorder != null,
                        preferences.motionMode == MotionMode.REDUCED, onEdit = { onEdit(shortcut.id) })
                        .testTag("home-editor-row-${shortcut.id}"), verticalAlignment = Alignment.CenterVertically) {
                        SettingsAction(shortcut.name, apps.find { it.target == shortcut.app }?.label ?:
                            shortcut.command?.label ?: if (shortcut.app == null) "Choose an action" else "App unavailable", { onEdit(shortcut.id) },
                            modifier = Modifier.weight(1f), enabled = reorder.draggingId == null, showChevron = false,
                            leading = { ShortcutGlyph(shortcut.icon, Modifier.size(24.dp)) })
                        Box {
                            IconButton(onClick = { arranging = shortcut.id }, enabled = reorder.draggingId == null, modifier = Modifier.size(48.dp)) {
                                Icon(painterResource(R.drawable.ic_material_drag_indicator), "Arrange ${shortcut.name}")
                            }
                            DropdownMenu(arranging == shortcut.id, { arranging = null }) {
                                DropdownMenuItem(text = { Text("Move up") }, leadingIcon = { Icon(upVector, null) }, enabled = index > 0,
                                    onClick = { arranging = null; onMove(shortcut.id, -1) })
                                DropdownMenuItem(text = { Text("Move down") }, leadingIcon = { Icon(downVector, null) }, enabled = index < preferences.homeShortcuts.lastIndex,
                                    onClick = { arranging = null; onMove(shortcut.id, 1) })
                            }
                        }
                    }
                }
            } }
        }
    }
    OutlinedButton(onClick = { onEdit(null) }, enabled = preferences.homeShortcuts.size < MAX_HOME_SHORTCUTS,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 12.dp)) { Text("Add shortcut") }
    SettingsDescription("${preferences.homeShortcuts.size} of $MAX_HOME_SHORTCUTS shortcuts · Swipe up on Home to open Apps",
        Modifier.padding(top = 12.dp))
}

@Composable
internal fun HomeShortcutEditorScreen(
    draft: HomeShortcutDraft, apps: List<AppEntry>, saving: Boolean,
    onChange: ((HomeShortcut) -> HomeShortcut) -> Unit, onChooseApp: () -> Unit,
    onSave: () -> Unit, onRemove: () -> Unit, onBack: () -> Unit,
) {
    var choosingIcon by rememberSaveable { mutableStateOf(false) }
    var choosingCommand by rememberSaveable { mutableStateOf(false) }
    var removing by rememberSaveable { mutableStateOf(false) }
    BackHandler { if (!saving) onBack() }
    val shortcut = draft.shortcut
    SettingsLayout(if (draft.isNew) "Add Home shortcut" else "Edit Home shortcut", { if (!saving) onBack() }) {
        SectionLabel("Action")
        ChoiceRow(listOf(false to "App", true to "Command"), shortcut.command != null, { command ->
            if (!saving) { if (command) choosingCommand = true else onChooseApp() }
        })
        if (shortcut.command != null) SettingsAction("Command", shortcut.command.label, { choosingCommand = true }, enabled = !saving)
        else SettingsAction("Opens", apps.find { it.target == shortcut.app }?.label ?:
            if (shortcut.app == null) "Choose an app" else "App unavailable", onChooseApp, enabled = !saving)
        SectionLabel("On Home")
        OutlinedTextField(value = shortcut.name, onValueChange = { value ->
            if (value.length <= MAX_HOME_NAME_LENGTH && value.none { it.isISOControl() }) onChange { it.copy(name = value) }
        }, label = { Text("Name") }, singleLine = true, enabled = !saving,
            modifier = Modifier.fillMaxWidth().testTag("home-shortcut-name"))
        SettingsAction("Icon", shortcut.icon.label, { choosingIcon = true }, enabled = !saving,
            leading = { ShortcutGlyph(shortcut.icon, Modifier.size(24.dp)) })
        SettingsDescription("Changing the action keeps your chosen name and icon. Names apply to Home only.")
        Button(onClick = onSave, enabled = draft.canSave && !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 24.dp)) { Text("Save") }
        if (!draft.isNew) TextButton(onClick = { removing = true }, enabled = !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Remove from Home") }
    }
    if (choosingCommand) ShortcutCommandPicker(shortcut.command, { choosingCommand = false }) { command ->
        onChange { it.copy(app = null, command = command,
            name = it.name.ifBlank { command.defaultName }, icon = if (it.name.isBlank()) command.icon else it.icon) }
        choosingCommand = false
    }
    if (choosingIcon) ShortcutIconPicker("Shortcut icon", shortcut.icon, { choosingIcon = false }) { icon ->
        onChange { it.copy(icon = icon) }; choosingIcon = false
    }
    if (removing) AlertDialog(onDismissRequest = { removing = false }, title = { Text("Remove ${shortcut.name}?") },
        text = { Text(if (shortcut.command == null) "The app stays available in Apps. You can add it to Home again."
            else "You can add this command again at any time.") },
        confirmButton = { TextButton(onClick = { removing = false; onRemove() }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { removing = false }) { Text("Cancel") } })
}
