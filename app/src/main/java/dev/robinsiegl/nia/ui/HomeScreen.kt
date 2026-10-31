package dev.robinsiegl.nia.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.robinsiegl.nia.model.*

internal val HomeHeaderCollapse = SemanticsPropertyKey<Float>("HomeHeaderCollapse")

@Composable
fun HomeScreen(
    layout: HomeLayout, time: String, date: String,
    onClock: () -> Unit, onShortcut: (String) -> Unit, onApps: () -> Unit,
    textSize: HomeTextSize = HomeTextSize.DEFAULT, onSettings: () -> Unit = {},
    pixelClockLayout: Boolean = true, contentTopInset: Dp = 0.dp,
    leftShortcut: CornerShortcut = CornerShortcut(), rightShortcut: CornerShortcut = CornerShortcut(),
    onCorner: (CornerPosition) -> Unit = {}, cornerLabel: (CornerPosition) -> String = { it.label + " shortcut" },
    motionMode: MotionMode = MotionMode.SYSTEM, unlockRequest: UnlockRevealRequest? = null, deviceLocked: Boolean = false,
    interactionEnabled: Boolean = true, onScrollable: (Boolean) -> Unit = {}, onRevealReady: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier, revealAllowed: Boolean = true,
    shortcuts: List<HomeShortcut> = emptyList(), onDate: () -> Unit = {},
    scroll: ScrollState = rememberScrollState(), pullProgress: Float = 0f, gesturesEnabled: Boolean = true,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val reduced = motionMode == MotionMode.REDUCED
    val entries = shortcuts + listOf(null) // Apps is reserved and always last.
    val rows = if (layout == HomeLayout.GRID) (entries.size + 2) / 3 else entries.size
    val reveal = remember { Animatable(if (deviceLocked || unlockRequest?.pending == true) 0f else 1f) }
    LaunchedEffect(deviceLocked, unlockRequest, motionMode, revealAllowed) {
        if (deviceLocked) reveal.snapTo(0f)
        else if (unlockRequest?.pending == true) {
            reveal.snapTo(0f)
            scroll.scrollTo(0)
            if (revealAllowed) {
                withFrameNanos { }
                reveal.animateTo(1f, tween(if (reduced) 120 else 360 + rows * 24,
                    delayMillis = if (reduced) 0 else 120, easing = LinearEasing))
                unlockRequest.consume()
            }
        } else reveal.snapTo(1f)
    }
    val revealReady by remember { derivedStateOf { reveal.value == 1f } }
    // These reads must subscribe composition even while the parent's touch blocker is present.
    val shortcutsRevealed = revealReady && !deviceLocked
    val enabled = interactionEnabled && shortcutsRevealed
    val canScroll = layout == HomeLayout.LIST && scroll.maxValue > 0
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
        val nominalHeaderHeight = if (alignedClock) with(density) {
            PixelHeaderLayout.forWidth(maxWidth.toPx(), contentTopInset.toPx(), 48.dp.toPx()).height.toDp()
        } else (144 * textSize.scale * density.fontScale).dp
        val compactHeight = (64 * density.fontScale.coerceAtLeast(1f)).dp
        val compactFallback = layout == HomeLayout.GRID && maxHeight < nominalHeaderHeight + 32.dp + 80.dp + 48.dp * rows
        val expandedHeight = if (compactFallback) compactHeight else nominalHeaderHeight
        val collapseDistance = with(density) { (expandedHeight - compactHeight).coerceAtLeast(48.dp).toPx() }
        val scrolled by remember(scroll, canScroll, collapseDistance) {
            derivedStateOf { if (canScroll) (scroll.value / collapseDistance).coerceIn(0f, 1f) else 0f }
        }
        val collapse = maxOf(scrolled, pullProgress.coerceIn(0f, 1f))
        val compactActive = collapse >= 0.5f
        val cornerSpace = 80.dp
        val gap = 16.dp
        val availableGrid = maxHeight - expandedHeight - cornerSpace - 32.dp
        val gridCell = ((availableGrid - gap * (rows - 1)) / rows).coerceIn(48.dp, 88.dp)
        val gridGap = ((availableGrid - gridCell * rows) / (rows - 1).coerceAtLeast(1)).coerceIn(0.dp, 16.dp)

        Column(Modifier.fillMaxSize().padding(bottom = cornerSpace).semantics { this[HomeHeaderCollapse] = collapse }) {
            Box(Modifier.weight(1f, fill = false).fillMaxWidth().clipToBounds()) {
                Column(Modifier.fillMaxWidth().then(if (layout == HomeLayout.LIST)
                    Modifier.verticalScroll(scroll, enabled = gesturesEnabled).testTag("home-list") else Modifier)) {
                    Box(Modifier.graphicsLayer {
                        alpha = 1f - collapse
                        // Scroll supplies movement in list mode; the direct Apps pull has a small upward shift.
                        translationY = if (reduced || scrolled > 0) 0f else -24.dp.toPx() * collapse
                    }.then(if (compactActive) Modifier.clearAndSetSemantics {} else Modifier)) {
                        if (compactFallback) HomeCompactHeader(time, date, onClock, onDate, enabled && !compactActive)
                        else if (alignedClock) PixelClockHeader(time, date, onClock, onDate,
                            contentTopInset = contentTopInset, interactive = enabled && !compactActive)
                        else Column(Modifier.padding(horizontal = 28.dp).padding(top = 24.dp)) {
                            Text(time, fontSize = (56 * textSize.scale).sp, fontWeight = FontWeight.Light,
                                modifier = Modifier.testTag("home-clock").clickable(enabled = enabled && !compactActive, role = Role.Button, onClick = onClock)
                                    .heightIn(min = 64.dp).semantics { contentDescription = "Time, $time. Open Clock" })
                            Text(date, fontSize = (16 * textSize.scale).sp,
                                modifier = Modifier.testTag("home-date").clickable(enabled = enabled && !compactActive, role = Role.Button, onClick = onDate)
                                    .heightIn(min = 48.dp).wrapContentHeight(Alignment.CenterVertically)
                                    .semantics { contentDescription = "Date, $date. Open Calendar" })
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    Column(Modifier.padding(horizontal = 28.dp)) {
                        if (layout == HomeLayout.GRID) {
                            entries.chunked(3).forEachIndexed { index, row ->
                                Row(Modifier.fillMaxWidth().revealRow(reveal.asState(), index, rows, reduced), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { shortcut ->
                                        Box(Modifier.weight(1f).height(gridCell).shortcutButton(enabled, reduced) {
                                            if (shortcut == null) onApps() else onShortcut(shortcut.id)
                                        }.testTag("home-entry-${shortcut?.id ?: "apps"}")
                                            .semantics { contentDescription = shortcut?.name ?: "Apps" }, contentAlignment = Alignment.Center) {
                                            if (shortcut == null) AppsIcon(Modifier.size(30.dp)) else ShortcutGlyph(shortcut.icon, Modifier.size(30.dp))
                                        }
                                    }
                                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                                if (index < rows - 1) Spacer(Modifier.height(gridGap))
                            }
                        } else shortcuts.forEachIndexed { index, shortcut ->
                            HomeListEntry(shortcut.name, enabled, reduced, textSize,
                                Modifier.revealRow(reveal.asState(), index, rows, reduced).testTag("home-entry-${shortcut.name}")) {
                                onShortcut(shortcut.id)
                            }
                        }
                    }
                }
                if (collapse > 0f) {
                    HomeCompactHeader(time, date, onClock, onDate, enabled && compactActive,
                        Modifier.graphicsLayer { alpha = collapse }
                            .then(if (compactActive) Modifier else Modifier.clearAndSetSemantics {}))
                }
            }
            if (layout == HomeLayout.LIST) HomeListEntry("Apps", enabled, reduced, textSize,
                Modifier.padding(horizontal = 28.dp).testTag(if (canScroll) "pinned-home-apps" else "home-entry-Apps")
                    .revealRow(reveal.asState(), rows - 1, rows, reduced), onApps)
        }
        listOf(CornerPosition.LEFT to leftShortcut, CornerPosition.RIGHT to rightShortcut).forEach { (position, shortcut) ->
            if (shortcut.action != ShortcutAction.EMPTY) Box(Modifier.align(
                if (position == CornerPosition.LEFT) AbsoluteAlignment.BottomLeft else AbsoluteAlignment.BottomRight)
                .padding(horizontal = 16.dp, vertical = 16.dp).size(48.dp)
                .revealRow(reveal.asState(), rows, rows, reduced).testTag("home-corner-" + position.name)
                .shortcutButton(enabled, reduced) { onCorner(position) }
                .semantics { contentDescription = cornerLabel(position) }, contentAlignment = Alignment.Center) {
                ShortcutGlyph(shortcut.icon, Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun HomeCompactHeader(time: String, date: String, onClock: () -> Unit, onDate: () -> Unit,
    enabled: Boolean, modifier: Modifier = Modifier) {
    val minimumHeight = (64 * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp
    Row(modifier.fillMaxWidth().heightIn(min = minimumHeight).background(MaterialTheme.colorScheme.background)
        .padding(horizontal = 28.dp).testTag("compact-home-header"), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(time, fontSize = 26.sp, fontWeight = FontWeight.Light,
            modifier = Modifier.heightIn(min = 48.dp).wrapContentHeight().clickable(enabled = enabled,
                role = Role.Button, onClick = onClock).semantics { contentDescription = "Time, $time. Open Clock" })
        Text(date, fontSize = 14.sp, modifier = Modifier.weight(1f).heightIn(min = 48.dp).wrapContentHeight()
            .clickable(enabled = enabled, role = Role.Button, onClick = onDate)
            .semantics { contentDescription = "Date, $date. Open Calendar" })
    }
}

@Composable
private fun HomeListEntry(name: String, enabled: Boolean, reduced: Boolean, textSize: HomeTextSize,
    modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(name, fontSize = (29 * textSize.scale).sp, fontWeight = FontWeight.Light,
        modifier = modifier.fillMaxWidth().heightIn(min = (60 * textSize.scale).dp)
            .shortcutButton(enabled, reduced, onClick).padding(vertical = 8.dp).wrapContentHeight(Alignment.CenterVertically))
}

private fun Modifier.revealRow(progress: State<Float>, index: Int, rows: Int, reduced: Boolean): Modifier = graphicsLayer {
    val fraction = if (reduced) progress.value else ((progress.value * (360 + rows * 24) - index * 24) / 360f).coerceIn(0f, 1f)
    val eased = FastOutSlowInEasing.transform(fraction)
    alpha = eased
    translationY = if (reduced) 0f else 16.dp.toPx() * (1f - eased)
}
