package dev.robinsiegl.nia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.robinsiegl.nia.model.ShortcutCommand

@Composable
internal fun ShortcutCommandPicker(selected: ShortcutCommand?, onDismiss: () -> Unit, onSelect: (ShortcutCommand) -> Unit) {
    val parentFocus = LocalFocusManager.current
    val parentKeyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(parentFocus) { parentFocus.clearFocus(); parentKeyboard?.hide() }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Choose command") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
            ShortcutCommand.entries.forEach { command ->
                Row(Modifier.fillMaxWidth().heightIn(min = 72.dp)
                    .selectable(command == selected, role = Role.RadioButton, onClick = { onSelect(command) })
                    .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ShortcutGlyph(command.icon, Modifier.size(24.dp))
                    Column(Modifier.weight(1f)) {
                        Text(command.label, style = MaterialTheme.typography.bodyLarge)
                        SettingsDescription(command.description, Modifier.padding(top = 4.dp))
                    }
                    RadioButton(selected = command == selected, onClick = null)
                }
            }
        }
    }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
