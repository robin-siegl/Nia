package dev.robinsiegl.nia.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.robinsiegl.nia.model.HomeCategory
import dev.robinsiegl.nia.model.HomeLayout
import dev.robinsiegl.nia.model.HomeTextSize

@Composable
fun HomeScreen(
    layout: HomeLayout, time: String, date: String,
    onClock: () -> Unit, onCategory: (HomeCategory) -> Unit, onApps: () -> Unit,
    textSize: HomeTextSize = HomeTextSize.DEFAULT, onSettings: () -> Unit = {},
    pixelClockLayout: Boolean = true, contentTopInset: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }
    var drag by remember { mutableFloatStateOf(0f) }
    val scroll = rememberScrollState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val alignedClock = pixelClockLayout && maxHeight > maxWidth
        Column(
            Modifier.fillMaxSize().pointerInput(onApps) {
                detectVerticalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = { if (drag < -threshold) onApps() },
                    onVerticalDrag = { _, amount -> drag += amount },
                )
            }.verticalScroll(scroll, enabled = scroll.maxValue > 0).padding(bottom = 88.dp),
        ) {
            if (alignedClock) {
                PixelClockHeader(time, date, onClock, { onCategory(HomeCategory.CALENDAR) }, contentTopInset = contentTopInset)
            } else Column(Modifier.padding(horizontal = 28.dp).padding(top = 24.dp)) {
                Text(time, fontSize = (56 * textSize.scale).sp, fontWeight = FontWeight.Light,
                    modifier = Modifier.clickable(role = Role.Button, onClick = onClock).heightIn(min = 64.dp)
                        .semantics { contentDescription = "Time, $time. Open Clock" })
                Text(date, fontSize = (16 * textSize.scale).sp,
                    modifier = Modifier.clickable(role = Role.Button) { onCategory(HomeCategory.CALENDAR) }
                        .heightIn(min = 48.dp).wrapContentHeight(Alignment.CenterVertically)
                        .semantics { contentDescription = "Date, $date. Open Calendar" })
            }
            Spacer(Modifier.height(32.dp))
            Column(Modifier.padding(horizontal = 28.dp)) {
                if (layout == HomeLayout.GRID) {
                    val shortcuts = HomeCategory.entries.toList() + listOf(null)
                    shortcuts.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { category ->
                                Box(Modifier.weight(1f).height(88.dp).clickable(role = Role.Button) {
                                    if (category == null) onApps() else onCategory(category)
                                }.semantics { contentDescription = category?.label ?: "Apps" }, contentAlignment = Alignment.Center) {
                                    if (category == null) AppsIcon(Modifier.size(30.dp)) else CategoryIcon(category, Modifier.size(30.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                } else {
                    HomeCategory.entries.forEach { category ->
                        HomeTextEntry(category.label, textSize) { onCategory(category) }
                    }
                    HomeTextEntry("Apps", textSize, onApps)
                }
            }
        }
        SettingsButton(onSettings, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp))
    }
}

@Composable
private fun HomeTextEntry(label: String, size: HomeTextSize, onClick: () -> Unit) {
    Text(label, fontSize = (29 * size.scale).sp, fontWeight = FontWeight.Light,
        modifier = Modifier.testTag("home-entry-$label").fillMaxWidth().heightIn(min = (60 * size.scale).dp)
            .clickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp)
            .wrapContentHeight(Alignment.CenterVertically))
}
