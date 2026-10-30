package dev.robinsiegl.nia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.robinsiegl.nia.model.*

@Composable
internal fun CornerSettings(preferences: LauncherPreferences, apps: List<AppEntry>,
    onChange: ((LauncherPreferences) -> LauncherPreferences) -> Unit, onAssign: (CornerPosition) -> Unit) {
    var choosingIcon by remember { mutableStateOf<CornerPosition?>(null) }
    Text("Two optional shortcuts at the bottom of Home. Settings stays available in Apps and by holding empty Home space.", fontSize = 14.sp)
    CornerPosition.entries.forEach { position ->
        val shortcut = preferences.shortcut(position)
        SectionLabel("${position.label.uppercase()} CORNER")
        Column(Modifier.testTag("corner-settings-${position.name}")) {
            ChoiceRow(listOf(ShortcutAction.EMPTY to "Empty", ShortcutAction.SETTINGS to "Settings", ShortcutAction.APP to "App"),
                shortcut.action, { action ->
                    if (action == ShortcutAction.APP) onAssign(position)
                    else onChange { it.withShortcut(position, it.shortcut(position).copy(action = action, app = null)) }
                })
            if (shortcut.action == ShortcutAction.APP) {
                val label = apps.find { it.target == shortcut.app }?.label ?: "Unavailable · choose another"
                TextButton(onClick = { onAssign(position) }, modifier = Modifier.fillMaxWidth()) { Text("App: $label") }
                TextButton(onClick = { choosingIcon = position }, modifier = Modifier.fillMaxWidth()) {
                    ShortcutGlyph(shortcut.icon, Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Icon: ${shortcut.icon.label}")
                }
            }
        }
    }
    choosingIcon?.let { position ->
        AlertDialog(onDismissRequest = { choosingIcon = null }, title = { Text("${position.label} shortcut icon") },
            text = {
                Column {
                    ShortcutIcon.entries.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { icon ->
                                IconButton(onClick = {
                                    onChange { it.withShortcut(position, it.shortcut(position).copy(icon = icon)) }; choosingIcon = null
                                }, modifier = Modifier.size(48.dp).semantics {
                                    contentDescription = icon.label
                                    selected = icon == preferences.shortcut(position).icon
                                }) { ShortcutGlyph(icon, Modifier.size(26.dp)) }
                            }
                        }
                    }
                }
            }, confirmButton = { TextButton(onClick = { choosingIcon = null }) { Text("Cancel") } })
    }
}
