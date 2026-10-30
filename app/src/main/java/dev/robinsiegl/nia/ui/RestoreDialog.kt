package dev.robinsiegl.nia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.robinsiegl.nia.model.RestorePreview

@Composable
internal fun RestoreDialog(preview: RestorePreview, restoring: Boolean, onRestore: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!restoring) onCancel() }, title = { Text("Review restore") },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()).testTag("restore-changes")) {
                val count = preview.changes.size
                Text(if (count == 0) "No settings will change." else "$count ${if (count == 1) "change" else "changes"} from this backup.")
                if (preview.unavailable.isNotEmpty()) {
                    Text("UNAVAILABLE APPS", fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                    Text("These assignments will be cleared. Choose their apps again after restoring.", fontSize = 14.sp)
                    preview.unavailable.forEach { missing ->
                        Text("${missing.label}: ${missing.target.key}", fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                }
                preview.changes.groupBy { it.section }.forEach { (section, changes) ->
                    Text(section.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                    changes.forEach { change ->
                        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                            Text(change.label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text("Current: ${change.before}", fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                            Text("Backup: ${change.after}", fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onRestore, enabled = !restoring) { Text(if (restoring) "Restoring…" else "Restore") } },
        dismissButton = { TextButton(onClick = onCancel, enabled = !restoring) { Text("Cancel") } })
}
