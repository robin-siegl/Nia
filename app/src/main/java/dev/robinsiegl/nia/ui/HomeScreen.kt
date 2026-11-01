package dev.robinsiegl.nia.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.robinsiegl.nia.model.*

internal val HomeHeaderCollapse = SemanticsPropertyKey<Float>("HomeHeaderCollapse")

@Composable
fun HomeScreen(
    layout: HomeLayout, time: String, date: String,
    onClock: () -> Unit, onShortcut: (String) -> Unit,
    textSize: HomeTextSize = HomeTextSize.DEFAULT, onSettings: () -> Unit = {},
    pixelClockLayout: Boolean = true, contentTopInset: Dp = 0.dp,
    leftShortcut: CornerShortcut = CornerShortcut(), rightShortcut: CornerShortcut = CornerShortcut(),
    onCorner: (CornerPosition) -> Unit = {}, cornerLabel: (CornerPosition) -> String = { it.label + " shortcut" },
    motionMode: MotionMode = MotionMode.SYSTEM, unlockRequest: UnlockRevealRequest? = null, deviceLocked: Boolean = false,
    interactionEnabled: Boolean = true, onScrollable: (Boolean) -> Unit = {}, onRevealReady: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier, revealAllowed: Boolean = true,
    shortcuts: List<HomeShortcut> = emptyList(), onDate: () -> Unit = {},
    scroll: ScrollState = rememberScrollState(), headerCollapse: Float = 0f, gesturesEnabled: Boolean = true,
    onHeaderRange: (Float) -> Unit = {}, onReorder: (suspend (List<String>) -> Boolean)? = null,
    onReorderActive: (Boolean) -> Unit = {}, onEditShortcut: ((String) -> Unit)? = null,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val reduced = motionMode == MotionMode.REDUCED
    val rows = (if (layout == HomeLayout.GRID) (shortcuts.size + 2) / 3 else shortcuts.size).coerceAtLeast(1)
    val reorder = rememberShortcutReorder(shortcuts, onReorder, onReorderActive,
        autoScroll = if (layout == HomeLayout.LIST) ({ delta -> scroll.scrollBy(delta) }) else null)
    val entries = remember(shortcuts, reorder.preview) { reorder.ordered(shortcuts) }
    LaunchedEffect(scroll) { reorder.cancel() }
    val reveal = remember { Animatable(if (deviceLocked || unlockRequest?.pending == true) 0f else 1f) }
    LaunchedEffect(deviceLocked, unlockRequest, motionMode, revealAllowed) {
        if (deviceLocked) { reorder.cancel(); reveal.snapTo(0f) }
        else if (unlockRequest?.pending == true) {
            reorder.cancel(); reveal.snapTo(0f); scroll.scrollTo(0)
            if (revealAllowed) {
                withFrameNanos { }
                reveal.animateTo(1f, tween(if (reduced) 120 else 360 + rows * 24,
                    delayMillis = if (reduced) 0 else 120, easing = LinearEasing))
                unlockRequest.consume()
            }
        } else reveal.snapTo(1f)
    }
    val revealReady by remember { derivedStateOf { reveal.value == 1f } }
    val shortcutsRevealed = revealReady && !deviceLocked
    val enabled = interactionEnabled && shortcutsRevealed
    val canLaunch = enabled && reorder.draggingId == null
    val settingsNow by rememberUpdatedState(onSettings)
    val canLaunchNow by rememberUpdatedState(canLaunch)
    // Keep these reads subscribed even while a transition is blocking new input.
    val canScroll = layout == HomeLayout.LIST && scroll.maxValue > 0
    SideEffect { onScrollable(canScroll); onRevealReady(shortcutsRevealed) }
    BoxWithConstraints(modifier.fillMaxSize().testTag("home-screen")
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown()
                if (canLaunchNow && awaitLongPressOrCancellation(down.id) != null && canLaunchNow) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    settingsNow()
                }
            }
        }) {
        val header = rememberClockHeader(time, date, with(density) { maxWidth.toPx() },
            pixelClockLayout && maxHeight > maxWidth, textSize, contentTopInset)
        val cornerSpace = 80.dp
        val gap = 24.dp
        val expanded = with(density) { header.expandedHeight.toDp() }
        val compact = with(density) { header.compactHeight.toDp() }
        val fallback = layout == HomeLayout.GRID && maxHeight < expanded + gap + cornerSpace + 48.dp * rows
        val expandedAtRest = if (fallback) compact else expanded
        val range = if (fallback) 0f else (header.expandedHeight - header.compactHeight).coerceAtLeast(0f)
        val collapse = if (fallback) 1f else headerCollapse.coerceIn(0f, 1f)
        SideEffect { onHeaderRange(range) }
        // Keep cell sizes stable while the header contracts, so the whole grid moves upward.
        val availableGrid = (maxHeight - expandedAtRest - cornerSpace - gap).coerceAtLeast(48.dp * rows)
        val cell = ((availableGrid - 16.dp * (rows - 1)) / rows).coerceIn(48.dp, 88.dp)
        val rowGap = ((availableGrid - cell * rows) / (rows - 1).coerceAtLeast(1)).coerceIn(0.dp, 16.dp)
        Column(Modifier.fillMaxSize().padding(bottom = cornerSpace).semantics { this[HomeHeaderCollapse] = collapse }) {
            MorphingClockHeader(header, time, date, collapse, onClock, onDate, canLaunch)
            Spacer(Modifier.height(gap))
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().reorderViewport(reorder)) {
                LookaheadScope {
                    val lookahead = this
                    if (layout == HomeLayout.GRID) {
                        ShortcutGrid(cell, rowGap, Modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
                            entries.forEachIndexed { index, shortcut -> key(shortcut.id) {
                                Box(Modifier.reorderSlot(shortcut.id, reorder)) {
                                Box(Modifier.fillMaxSize()
                                    .reorderShortcut(shortcut, reorder, lookahead, enabled && onReorder != null, reduced,
                                        onEdit = onEditShortcut?.let { { it(shortcut.id) } })
                                    .revealRow(reveal.asState(), index / 3, rows, reduced)
                                    .shortcutButton(canLaunch, reduced) { onShortcut(shortcut.id) }
                                    .testTag("home-entry-${shortcut.id}").semantics { contentDescription = shortcut.name },
                                    contentAlignment = Alignment.Center) {
                                    ShortcutGlyph(shortcut.icon, Modifier.size(30.dp))
                                }
                                }
                            } }
                        }
                    } else {
                        Column(Modifier.fillMaxSize().verticalScroll(scroll, enabled = gesturesEnabled && reorder.draggingId == null)
                            .testTag("home-list").padding(horizontal = 28.dp)) {
                            entries.forEachIndexed { index, shortcut -> key(shortcut.id) {
                                Box(Modifier.fillMaxWidth().reorderSlot(shortcut.id, reorder)) {
                                Text(shortcut.name, fontSize = (29 * textSize.scale).sp, fontWeight = FontWeight.Light,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = (60 * textSize.scale).dp)
                                        .reorderShortcut(shortcut, reorder, lookahead, enabled && onReorder != null, reduced,
                                            onEdit = onEditShortcut?.let { { it(shortcut.id) } })
                                        .revealRow(reveal.asState(), index, rows, reduced)
                                        .shortcutButton(canLaunch, reduced) { onShortcut(shortcut.id) }
                                        .padding(vertical = 8.dp).wrapContentHeight(Alignment.CenterVertically)
                                        .testTag("home-entry-${shortcut.id}").semantics { contentDescription = shortcut.name })
                                }
                            } }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
        listOf(CornerPosition.LEFT to leftShortcut, CornerPosition.RIGHT to rightShortcut).forEach { (position, shortcut) ->
            if (shortcut.action != ShortcutAction.EMPTY) Box(Modifier.align(
                if (position == CornerPosition.LEFT) AbsoluteAlignment.BottomLeft else AbsoluteAlignment.BottomRight)
                .padding(horizontal = 16.dp, vertical = 16.dp).size(48.dp)
                .revealRow(reveal.asState(), rows, rows, reduced).testTag("home-corner-" + position.name)
                .shortcutButton(canLaunch, reduced) { onCorner(position) }
                .semantics { contentDescription = cornerLabel(position) }, contentAlignment = Alignment.Center) {
                ShortcutGlyph(shortcut.icon, Modifier.size(24.dp))
            }
        }
    }
}

/** Fixed three-column placement keeps all nine targets even at pixel-rounded compact heights. */
@Composable
private fun ShortcutGrid(cell: Dp, rowGap: Dp, modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val rows = ((measurables.size + 2) / 3).coerceAtLeast(1)
        val horizontalGap = 8.dp.roundToPx()
        val width = ((constraints.maxWidth - horizontalGap * 2) / 3).coerceAtLeast(0)
        val gap = rowGap.roundToPx()
        val available = (constraints.maxHeight - gap * (rows - 1)).coerceAtLeast(0)
        val height = minOf(cell.roundToPx(), available / rows)
        val children = measurables.map { it.measure(Constraints.fixed(width, height)) }
        layout(constraints.maxWidth, height * rows + gap * (rows - 1)) {
            children.forEachIndexed { index, child -> child.placeRelative((index % 3) * (width + horizontalGap), (index / 3) * (height + gap)) }
        }
    }
}

private fun Modifier.revealRow(progress: State<Float>, index: Int, rows: Int, reduced: Boolean): Modifier = graphicsLayer {
    val fraction = if (reduced) progress.value else ((progress.value * (360 + rows * 24) - index * 24) / 360f).coerceIn(0f, 1f)
    val eased = FastOutSlowInEasing.transform(fraction)
    alpha = eased
    translationY = if (reduced) 0f else 16.dp.toPx() * (1f - eased)
}
