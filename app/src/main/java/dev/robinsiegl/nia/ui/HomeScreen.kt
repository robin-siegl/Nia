package dev.robinsiegl.nia.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.robinsiegl.nia.model.*

@Composable
fun HomeScreen(
    layout: HomeLayout, time: String, date: String,
    onClock: () -> Unit, onCategory: (HomeCategory) -> Unit, onApps: () -> Unit,
    textSize: HomeTextSize = HomeTextSize.DEFAULT, onSettings: () -> Unit = {},
    pixelClockLayout: Boolean = true, contentTopInset: Dp = 0.dp,
    leftShortcut: CornerShortcut = CornerShortcut(), rightShortcut: CornerShortcut = CornerShortcut(),
    onCorner: (CornerPosition) -> Unit = {}, cornerLabel: (CornerPosition) -> String = { it.label + " shortcut" },
    motionMode: MotionMode = MotionMode.SYSTEM, unlockRequest: UnlockRevealRequest? = null, deviceLocked: Boolean = false,
    interactionEnabled: Boolean = true, onScrollable: (Boolean) -> Unit = {}, onRevealReady: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val view = LocalView.current
    val reduced = motionMode == MotionMode.REDUCED
    val rows = if (layout == HomeLayout.GRID) 2 else 6
    val reveal = remember { Animatable(if (deviceLocked || unlockRequest?.pending == true) 0f else 1f) }
    LaunchedEffect(deviceLocked, unlockRequest, motionMode) {
        if (deviceLocked) reveal.snapTo(0f)
        else if (unlockRequest?.consume() == true) {
            reveal.snapTo(0f)
            // Draw a stable header before revealing shortcuts. The scaled start delay keeps
            // their first frames out of the system keyguard fade; scale=0 skips it entirely.
            withFrameNanos { }
            reveal.animateTo(1f, tween(if (reduced) 120 else 360 + rows * 24,
                delayMillis = if (reduced) 0 else 120, easing = LinearEasing))
        } else reveal.snapTo(1f)
    }
    val revealReady by remember { derivedStateOf { reveal.value == 1f } }
    // Observe completion even while the parent's input blocker is active. Reading it only
    // inside SideEffect leaves that blocker stale until an unrelated Home recomposition.
    val shortcutsRevealed = revealReady && !deviceLocked
    val enabled = interactionEnabled && shortcutsRevealed
    // Observe measurement here so the parent is updated when maxValue changes after first layout.
    val canScroll = scroll.maxValue > 0
    SideEffect { onScrollable(canScroll); onRevealReady(shortcutsRevealed) }
    BoxWithConstraints(modifier.fillMaxSize().testTag("home-screen").then(
        if (enabled) Modifier.pointerInput(onSettings) {
            awaitEachGesture {
                val down = awaitFirstDown()
                val press = awaitLongPressOrCancellation(down.id)
                if (press != null) {
                    press.consume()
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onSettings()
                }
            }
        } else Modifier,
    )) {
        val alignedClock = pixelClockLayout && maxHeight > maxWidth
        Column(Modifier.fillMaxSize().verticalScroll(scroll, enabled = canScroll).padding(bottom = 88.dp)) {
            if (alignedClock) {
                PixelClockHeader(time, date, onClock, { onCategory(HomeCategory.CALENDAR) },
                    contentTopInset = contentTopInset, interactive = enabled)
            } else Column(Modifier.padding(horizontal = 28.dp).padding(top = 24.dp)) {
                Text(time, fontSize = (56 * textSize.scale).sp, fontWeight = FontWeight.Light,
                    modifier = Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClock).heightIn(min = 64.dp)
                        .semantics { contentDescription = "Time, $time. Open Clock" })
                Text(date, fontSize = (16 * textSize.scale).sp,
                    modifier = Modifier.clickable(enabled = enabled, role = Role.Button) { onCategory(HomeCategory.CALENDAR) }
                        .heightIn(min = 48.dp).wrapContentHeight(Alignment.CenterVertically)
                        .semantics { contentDescription = "Date, $date. Open Calendar" })
            }
            Spacer(Modifier.height(32.dp))
            Column(Modifier.padding(horizontal = 28.dp)) {
                if (layout == HomeLayout.GRID) {
                    val shortcuts = HomeCategory.entries.toList() + listOf(null)
                    shortcuts.chunked(3).forEachIndexed { index, row ->
                        Row(Modifier.fillMaxWidth().revealRow(reveal.asState(), index, rows, reduced),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { category ->
                                Box(Modifier.weight(1f).height(88.dp).shortcutButton(enabled, reduced) {
                                    if (category == null) onApps() else onCategory(category)
                                }.semantics { contentDescription = category?.label ?: "Apps" }, contentAlignment = Alignment.Center) {
                                    if (category == null) AppsIcon(Modifier.size(30.dp)) else CategoryIcon(category, Modifier.size(30.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                } else {
                    (HomeCategory.entries.map { it.label to it } + listOf("Apps" to null)).forEachIndexed { index, (label, category) ->
                        Text(label, fontSize = (29 * textSize.scale).sp, fontWeight = FontWeight.Light,
                            modifier = Modifier.testTag("home-entry-$label").fillMaxWidth().heightIn(min = (60 * textSize.scale).dp)
                                .revealRow(reveal.asState(), index, rows, reduced).shortcutButton(enabled, reduced) {
                                    if (category == null) onApps() else onCategory(category)
                                }.padding(vertical = 8.dp).wrapContentHeight(Alignment.CenterVertically))
                    }
                }
            }
        }
        listOf(CornerPosition.LEFT to leftShortcut, CornerPosition.RIGHT to rightShortcut).forEach { (position, shortcut) ->
            if (shortcut.action != ShortcutAction.EMPTY) {
                val label = cornerLabel(position)
                Box(Modifier.align(if (position == CornerPosition.LEFT) AbsoluteAlignment.BottomLeft else AbsoluteAlignment.BottomRight)
                    .padding(horizontal = 16.dp, vertical = 16.dp).size(48.dp)
                    .revealRow(reveal.asState(), rows, rows, reduced).testTag("home-corner-" + position.name)
                    .shortcutButton(enabled, reduced) { onCorner(position) }
                    .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
                    ShortcutGlyph(shortcut.icon, Modifier.size(24.dp))
                }
            }
        }
    }
}

private fun Modifier.revealRow(progress: State<Float>, index: Int, rows: Int, reduced: Boolean): Modifier = graphicsLayer {
    val fraction = if (reduced) progress.value else ((progress.value * (360 + rows * 24) - index * 24) / 360f).coerceIn(0f, 1f)
    val eased = FastOutSlowInEasing.transform(fraction)
    alpha = eased
    translationY = if (reduced) 0f else 16.dp.toPx() * (1f - eased)
}
