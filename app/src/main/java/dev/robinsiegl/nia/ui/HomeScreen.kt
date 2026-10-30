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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.robinsiegl.nia.model.HomeCategory
import dev.robinsiegl.nia.model.HomeLayout

@Composable
fun HomeScreen(
    layout: HomeLayout, time: String, date: String,
    onClock: () -> Unit, onCategory: (HomeCategory) -> Unit, onApps: () -> Unit,
) {
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }
    var drag by remember { mutableFloatStateOf(0f) }
    val scroll = rememberScrollState()
    Column(
        Modifier.fillMaxSize().pointerInput(onApps) {
            detectVerticalDragGestures(
                onDragStart = { drag = 0f },
                onDragEnd = { if (drag < -threshold) onApps() },
                onVerticalDrag = { _, amount -> drag += amount },
            )
        }.verticalScroll(scroll, enabled = scroll.maxValue > 0).padding(horizontal = 28.dp).padding(top = 24.dp, bottom = 24.dp),
    ) {
        Text(time, fontSize = 56.sp, fontWeight = FontWeight.Light,
            modifier = Modifier.clickable(role = Role.Button, onClick = onClock).heightIn(min = 64.dp)
                .semantics { contentDescription = "Time, $time. Open Clock" })
        Text(date, fontSize = 16.sp,
            modifier = Modifier.clickable(role = Role.Button) { onCategory(HomeCategory.CALENDAR) }
                .heightIn(min = 48.dp).wrapContentHeight(Alignment.CenterVertically)
                .semantics { contentDescription = "Date, $date. Open Calendar" })
        Spacer(Modifier.height(32.dp))
        if (layout == HomeLayout.GRID) {
            HomeCategory.entries.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { category ->
                        Box(Modifier.weight(1f).height(88.dp).clickable(role = Role.Button) { onCategory(category) }
                            .semantics { contentDescription = category.label }, contentAlignment = Alignment.Center) {
                            CategoryIcon(category, Modifier.size(30.dp))
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(16.dp))
            }
        } else {
            HomeCategory.entries.forEach { category ->
                Text(category.label, fontSize = 29.sp, fontWeight = FontWeight.Light,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
                        .clickable(role = Role.Button) { onCategory(category) }.wrapContentHeight(Alignment.CenterVertically))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Apps", fontSize = 15.sp, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onApps).wrapContentHeight(Alignment.CenterVertically))
    }
}
