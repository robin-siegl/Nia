package dev.robinsiegl.nia.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.robinsiegl.nia.model.*

@Composable
internal fun HomePreview(preferences: LauncherPreferences, modifier: Modifier = Modifier) {
    val scale = preferences.homeTextSize.scale
    Column(modifier.fillMaxWidth().testTag("home-preview")
        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(16.dp)) {
        Text("Home preview", fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        if (preferences.pixelClockLayout) {
            PixelClockHeader("09:41", "Fri, 30 Oct", {}, {}, interactive = false)
        } else {
            Text("09:41", fontSize = (36 * scale).sp, fontWeight = FontWeight.Light)
            Text("Friday, October 30", fontSize = (12 * scale).sp)
        }
        Spacer(Modifier.height(16.dp))
        if (preferences.layout == HomeLayout.GRID) {
            val shortcuts = HomeCategory.entries.toList() + listOf(null)
            shortcuts.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    row.forEach { category ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (category == null) AppsIcon(Modifier.size(22.dp)) else CategoryIcon(category, Modifier.size(22.dp))
                        }
                    }
                }
            }
        } else {
            (HomeCategory.entries.map { it.label } + "Apps").forEach { label ->
                Text(label, fontSize = (20 * scale).sp, fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(vertical = 3.dp))
            }
        }
        val corners = listOf(preferences.leftShortcut, preferences.rightShortcut)
        if (corners.any { it.action != ShortcutAction.EMPTY }) {
            Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                corners.forEach { shortcut ->
                    Box(Modifier.size(24.dp)) {
                        if (shortcut.action != ShortcutAction.EMPTY) ShortcutGlyph(
                            if (shortcut.action == ShortcutAction.SETTINGS) ShortcutIcon.SETTINGS else shortcut.icon, Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}
