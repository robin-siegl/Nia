package dev.robinsiegl.nia.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun CategoryIcon(category: HomeCategory, modifier: Modifier = Modifier) {
    Icon(categoryVectors.getValue(category), contentDescription = null, modifier = modifier)
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(end = 12.dp)) { Text("Back", fontSize = 14.sp) }
        Text(title, fontSize = 18.sp)
        if (trailing != null) trailing() else Spacer(Modifier.width(48.dp))
    }
}

@Composable
fun <T> ChoiceRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val active = selected == value
            OutlinedButton(
                onClick = { onSelect(value) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(6.dp), contentPadding = PaddingValues(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                ), border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = if (active) 1f else 0.25f)),
            ) { Text(label, fontSize = 13.sp) }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        modifier = Modifier.padding(top = 30.dp, bottom = 12.dp))
}
