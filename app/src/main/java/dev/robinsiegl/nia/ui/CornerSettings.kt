package dev.robinsiegl.nia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.robinsiegl.nia.model.*

@Composable
internal fun CornerSettings(preferences: LauncherPreferences, apps: List<AppEntry>,
    onChange: ((LauncherPreferences) -> LauncherPreferences) -> Unit, onAssign: (CornerPosition) -> Unit) {
    var choosingIcon by rememberSaveable { mutableStateOf<CornerPosition?>(null) }
    SettingsDescription("Leave a corner empty, or choose an app and an outline icon.", Modifier.padding(top = 8.dp))
    CornerPosition.entries.forEach { position ->
        val shortcut = preferences.shortcut(position)
        SectionLabel("${position.label} corner")
        Column(Modifier.testTag("corner-settings-${position.name}")) {
            ChoiceRow(listOf(ShortcutAction.EMPTY to "Empty", ShortcutAction.APP to "App"),
                shortcut.action, { action ->
                    if (action == ShortcutAction.APP) onAssign(position)
                    else onChange { it.withShortcut(position, it.shortcut(position).copy(action = action, app = null)) }
                })
            if (shortcut.action == ShortcutAction.APP) {
                val label = apps.find { it.target == shortcut.app }?.label ?: "App unavailable"
                SettingsAction("App", label, { onAssign(position) })
                SettingsAction("Icon", shortcut.icon.label, { choosingIcon = position },
                    leading = { ShortcutGlyph(shortcut.icon, Modifier.size(24.dp)) })
            }
        }
    }
    SettingsDescription("Open Settings from Apps, or touch and hold empty space on Home.", Modifier.padding(top = 24.dp))
    choosingIcon?.let { position ->
        ShortcutIconPicker("${position.label} shortcut icon", preferences.shortcut(position).icon,
            onDismiss = { choosingIcon = null }, onSelect = { icon ->
                onChange { it.withShortcut(position, it.shortcut(position).copy(icon = icon)) }; choosingIcon = null
            })
    }
}

@Composable
internal fun ShortcutIconPicker(title: String, selected: ShortcutIcon, onDismiss: () -> Unit, onSelect: (ShortcutIcon) -> Unit) {
    AlertDialog(onDismissRequest = { onDismiss() }, title = { Text(title) },
        text = {
            LazyVerticalGrid(columns = GridCells.Adaptive(56.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).selectableGroup().testTag("shortcut-icon-grid")) {
                items(ShortcutIcon.entries, key = { it.name }) { icon ->
                    Box(contentAlignment = Alignment.Center) {
                        val active = icon == selected
                        val shape = RoundedCornerShape(12.dp)
                        Box(modifier = Modifier.size(48.dp).clip(shape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (active) 0.12f else 0f))
                            .border(if (active) 1.dp else 0.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = if (active) 0.8f else 0f), shape)
                            .selectable(selected = active, role = Role.RadioButton, onClick = {
                            onSelect(icon)
                        }).semantics {
                            contentDescription = icon.label
                        }, contentAlignment = Alignment.Center) { ShortcutGlyph(icon, Modifier.size(26.dp)) }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { onDismiss() }) { Text("Cancel") } })
}
