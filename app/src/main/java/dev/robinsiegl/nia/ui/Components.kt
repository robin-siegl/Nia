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
import dev.robinsiegl.nia.R
import androidx.compose.ui.res.painterResource

internal fun outlineIcon(name: String, path: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    .addPath(addPathNodes(path), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round).build()

@Composable
fun AppsIcon(modifier: Modifier = Modifier) {
    Icon(painterResource(R.drawable.ic_material_apps), contentDescription = null, modifier = modifier)
}

@Composable
fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    IconButton(onClick, modifier = modifier.size(48.dp), enabled = enabled) {
        Icon(painterResource(R.drawable.ic_material_settings), contentDescription = "Settings", modifier = Modifier.size(24.dp))
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
