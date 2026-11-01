package dev.robinsiegl.nia.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import dev.robinsiegl.nia.model.HomeShortcut
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

/** A single drag preview shared by Home and Settings. Persist once on drop; cancel writes nothing. */
@Stable
internal class ShortcutReorderState(ids: List<String>, private val scope: CoroutineScope) {
    var sourceIds by mutableStateOf(ids)
        private set
    var preview by mutableStateOf<List<String>?>(null)
        private set
    var draggingId by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set
    var pointer by mutableStateOf(Offset.Zero)
        private set
    var viewport by mutableStateOf(Rect.Zero)
    private val slots = mutableMapOf<String, Rect>()
    private var grab = Offset.Zero
    var activeBounds by mutableStateOf(Rect.Zero)
        private set
    var onCommit: suspend (List<String>) -> Boolean = { false }
    val dragTranslation: Offset get() = pointer - activeBounds.topLeft - grab

    fun sync(ids: List<String>) {
        if (draggingId != null && sourceIds != ids) cancel()
        sourceIds = ids
        slots.keys.retainAll(ids.toSet())
        if (draggingId == null && !saving) preview = null
    }

    fun place(id: String, bounds: Rect) {
        slots[id] = bounds
        if (draggingId == id) activeBounds = bounds
    }

    fun start(id: String, local: Offset): Boolean {
        if (saving || draggingId != null || id !in sourceIds) return false
        val bounds = slots[id] ?: return false
        preview = sourceIds
        draggingId = id
        grab = local
        pointer = bounds.topLeft + local
        activeBounds = bounds
        return true
    }

    // Publish the pointer and proposed order together, before the next placement pass.
    fun drag(delta: Offset) = Snapshot.withMutableSnapshot { pointer += delta; updateTarget() }

    fun updateTarget() {
        val id = draggingId ?: return
        val order = preview ?: return
        val center = pointer - grab + Offset(activeBounds.width / 2f, activeBounds.height / 2f)
        val target = order.firstOrNull { it != id && slots[it]?.contains(center) == true } ?: return
        val next = order.toMutableList()
        val destination = next.indexOf(target)
        next.add(destination, next.removeAt(next.indexOf(id)))
        preview = next
    }

    fun cancel() { draggingId = null; preview = null }

    fun drop() {
        val order = preview
        if (draggingId == null || order == null || !viewport.contains(pointer) || order == sourceIds) { cancel(); return }
        draggingId = null
        commit(order)
    }

    fun move(id: String, direction: Int): Boolean {
        if (saving || draggingId != null) return false
        val next = sourceIds.toMutableList()
        val index = next.indexOf(id)
        val destination = index + direction
        if (index < 0 || destination !in next.indices) return false
        next.add(destination, next.removeAt(index))
        preview = next
        commit(next)
        return true
    }

    private fun commit(order: List<String>) {
        saving = true
        scope.launch {
            try {
                val accepted = onCommit(order)
                if (!accepted || sourceIds == order) preview = null
            } finally { saving = false }
        }
    }
}

@Composable
internal fun rememberShortcutReorder(
    shortcuts: List<HomeShortcut>, onCommit: (suspend (List<String>) -> Boolean)?,
    onActiveChanged: (Boolean) -> Unit = {}, autoScroll: (suspend (Float) -> Float)? = null,
): ShortcutReorderState {
    val scope = rememberCoroutineScope()
    val ids = shortcuts.map { it.id }
    val state = remember { ShortcutReorderState(ids, scope) }
    val currentCommit by rememberUpdatedState(onCommit)
    val activeChanged by rememberUpdatedState(onActiveChanged)
    val scrollNow by rememberUpdatedState(autoScroll)
    val density = LocalDensity.current
    LaunchedEffect(ids) { state.sync(ids) }
    SideEffect { state.onCommit = { currentCommit?.invoke(it) ?: false }; activeChanged(state.draggingId != null) }
    DisposableEffect(state) { onDispose { activeChanged(false) } }
    LaunchedEffect(state) {
        snapshotFlow { Triple(state.draggingId, state.pointer, state.viewport) }.collectLatest { (id, _, _) ->
            if (id == null || scrollNow == null) return@collectLatest
            val edge = with(density) { 48.dp.toPx() }
            fun speed(): Float {
                val bounds = state.viewport
                val y = state.pointer.y
                return when {
                    y < bounds.top + edge -> -((bounds.top + edge - y) / edge).coerceIn(0f, 1f)
                    y > bounds.bottom - edge -> ((y - bounds.bottom + edge) / edge).coerceIn(0f, 1f)
                    else -> 0f
                }
            }
            if (speed() == 0f) return@collectLatest
            var previous = withFrameNanos { it }
            while (state.draggingId != null && speed() != 0f) {
                val now = withFrameNanos { it }
                val seconds = ((now - previous) / 1_000_000_000f).coerceAtMost(0.032f)
                previous = now
                val consumed = scrollNow?.invoke(with(density) { 600.dp.toPx() } * seconds * speed()) ?: 0f
                state.updateTarget()
                if (consumed == 0f) break
            }
        }
    }
    return state
}

internal fun ShortcutReorderState.ordered(shortcuts: List<HomeShortcut>): List<HomeShortcut> {
    val entries = shortcuts.associateBy { it.id }
    if (preview == null || preview?.toSet() != entries.keys) return shortcuts
    return (preview ?: sourceIds).mapNotNull(entries::get)
}

internal fun Modifier.reorderViewport(state: ShortcutReorderState): Modifier = onGloballyPositioned {
    state.viewport = it.boundsInRoot()
}

/** Measure a separate, untransformed slot. Measuring the lifted layer feeds its own offset back into the drag. */
internal fun Modifier.reorderSlot(id: String, state: ShortcutReorderState): Modifier =
    onGloballyPositioned { state.place(id, Rect(it.positionInRoot(), it.size.toSize())) }
        .zIndex(if (state.draggingId == id) 1f else 0f)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.reorderShortcut(
    shortcut: HomeShortcut, state: ShortcutReorderState, lookahead: LookaheadScope,
    enabled: Boolean, reduced: Boolean, onEdit: (() -> Unit)? = null,
): Modifier {
    val view = LocalView.current
    val dragging = state.draggingId == shortcut.id
    val offset = if (dragging) state.dragTranslation else Offset.Zero
    val translation by animateOffsetAsState(offset, if (dragging || reduced) snap() else spring(stiffness = 500f), label = "Shortcut drop")
    val scale by animateFloatAsState(if (dragging && !reduced) 1.05f else 1f, label = "Shortcut lift")
    val ink = MaterialTheme.colorScheme.onSurface
    val order = state.preview ?: state.sourceIds
    val index = order.indexOf(shortcut.id)
    val actions = buildList {
        onEdit?.let { add(CustomAccessibilityAction("Edit ${shortcut.name}") { it(); true }) }
        if (enabled && index > 0) add(CustomAccessibilityAction("Move up") { state.move(shortcut.id, -1) })
        if (enabled && index < order.lastIndex) add(CustomAccessibilityAction("Move down") { state.move(shortcut.id, 1) })
    }
    return this.pointerInput(state, shortcut.id, enabled) {
        if (enabled) detectDragGesturesAfterLongPress(
            onDragStart = { if (state.start(shortcut.id, it)) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) },
            onDrag = { change, amount -> change.consume(); state.drag(amount) },
            onDragEnd = state::drop, onDragCancel = state::cancel,
        )
    }
        .then(if (dragging) Modifier else Modifier.animateBounds(lookahead,
            boundsTransform = { _, _ -> if (reduced) snap() else spring(stiffness = 450f) }))
        .offset {
            val point = if (state.draggingId == shortcut.id) state.dragTranslation else translation
            IntOffset(point.x.roundToInt(), point.y.roundToInt())
        }
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .background(ink.copy(alpha = if (dragging) 0.10f else 0f), RoundedCornerShape(16.dp))
        .semantics { stateDescription = "Position ${index + 1} of ${order.size}"; customActions = actions }
}
