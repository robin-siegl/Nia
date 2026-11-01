package dev.robinsiegl.nia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.robinsiegl.nia.model.*

@Composable
internal fun QuickCornerSettings(preferences: LauncherPreferences, apps: List<AppEntry>,
    onChange: ((LauncherPreferences) -> LauncherPreferences) -> Unit, onAssign: (CornerPosition) -> Unit) {
    var choosingIcon by rememberSaveable { mutableStateOf<CornerPosition?>(null) }
    var choosingCommand by rememberSaveable { mutableStateOf<CornerPosition?>(null) }
    SettingsDescription("Leave a corner empty, or choose an app or command and an outline icon.", Modifier.padding(top = 8.dp))
    CornerPosition.entries.forEach { position ->
        val shortcut = preferences.shortcut(position)
        SectionLabel("${position.label} corner")
        Column(Modifier.testTag("corner-settings-${position.name}")) {
            ChoiceRow(listOf(ShortcutAction.EMPTY to "Empty", ShortcutAction.APP to "App", ShortcutAction.COMMAND to "Command"),
                shortcut.action, { action ->
                    if (action == ShortcutAction.APP) onAssign(position)
                    else if (action == ShortcutAction.COMMAND) choosingCommand = position
                    else onChange { it.withShortcut(position, it.shortcut(position).copy(action = action, app = null, command = null)) }
                })
            if (shortcut.action == ShortcutAction.APP) {
                val label = apps.find { it.target == shortcut.app }?.label ?: "App unavailable"
                SettingsAction("App", label, { onAssign(position) })
            } else if (shortcut.action == ShortcutAction.COMMAND) {
                SettingsAction("Command", shortcut.command?.label ?: "Choose a command", { choosingCommand = position })
            }
            if (shortcut.action != ShortcutAction.EMPTY) {
                SettingsAction("Icon", shortcut.icon.label, { choosingIcon = position },
                    leading = { ShortcutGlyph(shortcut.icon, Modifier.size(24.dp)) })
            }
        }
    }
    choosingCommand?.let { position ->
        ShortcutCommandPicker(preferences.shortcut(position).command, { choosingCommand = null }) { command ->
            onChange { current ->
                val old = current.shortcut(position)
                current.withShortcut(position, old.copy(action = ShortcutAction.COMMAND, app = null, command = command,
                    icon = if (old.action == ShortcutAction.EMPTY) command.icon else old.icon))
            }
            choosingCommand = null
        }
    }
    SettingsDescription("Open Settings from Apps, or touch and hold empty space on Home.", Modifier.padding(top = 24.dp))
    choosingIcon?.let { position ->
        ShortcutIconPicker("${position.label} corner icon", preferences.shortcut(position).icon,
            onDismiss = { choosingIcon = null }, onSelect = { icon ->
                onChange { it.withShortcut(position, it.shortcut(position).copy(icon = icon)) }; choosingIcon = null
            })
    }
}
