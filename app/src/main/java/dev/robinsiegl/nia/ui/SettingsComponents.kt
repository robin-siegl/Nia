package dev.robinsiegl.nia.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val backVector = outlineIcon("Back", "M19,12H5 M12,5L5,12L12,19")
private val chevronVector = outlineIcon("Open", "M9,5L16,12L9,19")

/** Keep navigation visible while long settings pages scroll, including at large font sizes. */
@Composable
internal fun SettingsLayout(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(backVector, contentDescription = "Back")
                }
                Text(title, style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).padding(start = 12.dp, end = 12.dp).semantics { heading() })
            }
            key(title) {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .testTag("settings-content").padding(horizontal = 24.dp).padding(bottom = 32.dp), content = content)
            }
        }
    }
}

@Composable
internal fun SettingsAction(label: String, description: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, showChevron: Boolean = true,
    leading: (@Composable () -> Unit)? = null) {
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f)
    Row(modifier.fillMaxWidth().heightIn(min = if (description.isEmpty()) 56.dp else 72.dp)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
            if (description.isNotEmpty()) Text(description, style = MaterialTheme.typography.bodyMedium,
                color = color.copy(alpha = if (enabled) 0.72f else 0.38f), modifier = Modifier.padding(top = 4.dp))
        }
        if (showChevron) Icon(chevronVector, contentDescription = null, tint = color.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun SettingsDescription(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), modifier = modifier)
}
