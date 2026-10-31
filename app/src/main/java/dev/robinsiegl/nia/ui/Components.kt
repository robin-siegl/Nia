package dev.robinsiegl.nia.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.robinsiegl.nia.model.HomeCategory

private val categoryVectors = HomeCategory.entries.associateWith { category ->
    val path = when (category) {
        HomeCategory.PHONE -> "M7,3H4C3.4,3 3,3.5 3,4C3,13.4 10.6,21 20,21C20.5,21 21,20.6 21,20V17L16,15L14,17C10.9,15.6 8.4,13.1 7,10L9,8Z"
        HomeCategory.MESSAGES -> "M4,4H20V17H9L4,21Z M8,9H16 M8,13H13"
        HomeCategory.MUSIC -> "M9,17V5L20,3V15 M9,8L20,6 M9,17C9,21 3,22 3,18C3,14 9,14 9,17 M20,15C20,19 14,20 14,16C14,12 20,12 20,15"
        HomeCategory.CALENDAR -> "M4,5H20V21H4Z M4,10H20 M8,3V7 M16,3V7 M8,14H10 M14,14H16 M8,17H10"
        HomeCategory.MAPS -> "M3,5L9,3L15,5L21,3V19L15,21L9,19L3,21Z M9,3V19 M15,5V21"
    }
    ImageVector.Builder(category.label, 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes(path), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round).build()
}

internal fun outlineIcon(name: String, path: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    .addPath(addPathNodes(path), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round).build()

private val appsVector = outlineIcon("Apps", "M4,4H10V10H4Z M14,4H20V10H14Z M4,14H10V20H4Z M14,14H20V20H14Z")
internal val settingsVector = outlineIcon("Settings",
    "M9,3H15L15.7,5.3L17.4,6.3L19.7,5.8L21.2,8.4L19.6,10.1V13.9L21.2,15.6L19.7,18.2L17.4,17.7L15.7,18.7L15,21H9L8.3,18.7L6.6,17.7L4.3,18.2L2.8,15.6L4.4,13.9V10.1L2.8,8.4L4.3,5.8L6.6,6.3L8.3,5.3Z M16,12A4,4 0,1 1,8,12A4,4 0,1 1,16,12")

@Composable
fun AppsIcon(modifier: Modifier = Modifier) {
    Icon(appsVector, contentDescription = null, modifier = modifier)
}

@Composable
fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    IconButton(onClick, modifier = modifier.size(48.dp), enabled = enabled) {
        Icon(settingsVector, contentDescription = "Settings", modifier = Modifier.size(24.dp))
    }
}

/** A continuous selector that also accepts a single tap without touch-slop delay. */
@Composable
internal fun Modifier.dragToSelect(onSelect: (Offset, IntSize) -> Unit, onFinished: () -> Unit): Modifier {
    val select by rememberUpdatedState(onSelect)
    val finish by rememberUpdatedState(onFinished)
    return pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown()
            select(down.position, size)
            down.consume()
            do {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                select(change.position, size)
                change.consume()
            } while (change.pressed)
            finish()
        }
    }
}

@Composable
fun CategoryIcon(category: HomeCategory, modifier: Modifier = Modifier) {
    Icon(categoryVectors.getValue(category), contentDescription = null, modifier = modifier)
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        TextButton(onClick = onBack, enabled = enabled, contentPadding = PaddingValues(end = 12.dp)) { Text("Back", fontSize = 14.sp) }
        Text(title, fontSize = 18.sp)
        if (trailing != null) trailing() else Spacer(Modifier.width(48.dp))
    }
}

@Composable
fun <T> ChoiceRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val active = selected == value
            Surface(modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp)
                .selectable(selected = active, role = Role.RadioButton, onClick = { onSelect(value) }),
                shape = RoundedCornerShape(8.dp),
                color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = if (active) 1f else 0.3f)),
            ) {
                Box(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp).semantics { heading() })
}
