package dev.robinsiegl.nia.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.robinsiegl.nia.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

private class PointerTermination {
    var active = false
    var releasedNormally = false
    var startedOpen = false
}

internal val DrawerProgress = SemanticsPropertyKey<Float>("DrawerProgress")
internal val AppsPullArmed = SemanticsPropertyKey<Boolean>("AppsPullArmed")
internal val DrawerSettling = SemanticsPropertyKey<Boolean>("DrawerSettling")

internal fun drawerShouldOpen(progress: Float, velocityY: Float, velocityThreshold: Float): Boolean = when {
    velocityY < -velocityThreshold -> true
    velocityY > velocityThreshold -> false
    else -> progress >= 0.5f
}

@Stable
internal class DrawerMotion(initialOpen: Boolean, private val scope: CoroutineScope) {
    var progress by mutableFloatStateOf(if (initialOpen) 1f else 0f)
        private set
    var dragging by mutableStateOf(false)
        private set
    var settling by mutableStateOf(false)
        private set
    var targetOpen = initialOpen
        private set
    private var animation: Job? = null
    private var generation = 0

    fun snapClosed() {
        animation?.cancel(); generation++
        progress = 0f; targetOpen = false; dragging = false; settling = false
    }

    fun preview(value: Float) {
        if (!settling) { dragging = true; progress = value.coerceIn(0f, 1f) }
    }

    fun drag(delta: Float, travel: Float): Float {
        if (settling) return 0f
        dragging = true
        val before = progress
        progress = (progress - delta / travel).coerceIn(0f, 1f)
        return (before - progress) * travel
    }

    fun moveTo(open: Boolean, reduced: Boolean, onSettled: (Boolean) -> Unit) {
        if (settling && targetOpen == open) return
        animation?.cancel()
        val token = ++generation
        targetOpen = open
        dragging = false
        val target = if (open) 1f else 0f
        if (progress == target) { settling = false; onSettled(open); return }
        settling = true
        animation = scope.launch {
            try {
                animate(progress, target, animationSpec = tween(
                    if (reduced) 120 else maxOf(100, (280 * abs(target - progress)).toInt()),
                    easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
                )) { value, _ -> progress = value.coerceIn(0f, 1f) }
                progress = target
                settling = false
                onSettled(open)
            } finally { if (generation == token) settling = false }
        }
    }
}

/** Home stays mounted under the drawer. Gesture, tap and Back all share the same motion state. */
@Composable
internal fun HomeAppsScreen(
    preferences: LauncherPreferences, apps: List<AppEntry>, loading: Boolean, time: String, date: String,
    drawerOpen: Boolean, onDrawerOpenChanged: (Boolean) -> Unit,
    onClock: () -> Unit, onShortcut: (String) -> Unit, onSettings: () -> Unit,
    onApp: (AppEntry) -> Unit, onAppInfo: (AppEntry) -> Unit, onCorner: (CornerPosition) -> Unit,
    contentTopInset: Dp = 0.dp, unlockRequest: UnlockRevealRequest? = null, deviceLocked: Boolean = false, homeRequest: Int = 0,
    onAssignCorner: ((AppEntry, CornerPosition) -> Unit)? = null,
    revealAllowed: Boolean = true, onDate: () -> Unit = {},
    onPullCue: ((PullCue) -> Unit)? = null,
    onAddHome: ((AppEntry) -> Unit)? = null, searchAppsOnOpen: Boolean = false,
    onReorderHome: (suspend (List<String>) -> Boolean)? = null, onEditHome: ((String) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val state = remember { DrawerMotion(drawerOpen, scope) }
    val pull = remember { AppsPull() }
    val pointer = remember { PointerTermination() }
    // Reset before the first unlock/Home frame; keep the offset while Apps is temporarily on top.
    val homeScroll = key(homeRequest, unlockRequest) { rememberScrollState() }
    val homeHeader = key(homeRequest, unlockRequest) { remember { HomeHeaderScroll() } }
    val view = LocalView.current
    val cueNow by rememberUpdatedState(onPullCue)
    val externalOpen by rememberUpdatedState(drawerOpen)
    val onChanged by rememberUpdatedState(onDrawerOpenChanged)
    val reduced = preferences.motionMode == MotionMode.REDUCED
    val reducedNow by rememberUpdatedState(reduced)
    val lockedNow by rememberUpdatedState(deviceLocked)
    val density = LocalDensity.current
    val velocityThreshold = with(density) { 700.dp.toPx() }
    val pullThreshold = with(density) { 112.dp.toPx() }
    val listState = rememberLazyListState()
    var reordering by remember { mutableStateOf(false) }
    val reorderingNow by rememberUpdatedState(reordering)
    var shortcutsReady by remember { mutableStateOf(!deviceLocked) }
    var travel by remember { mutableFloatStateOf(1f) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    fun moved(open: Boolean) { if (externalOpen != open) onChanged(open) }
    fun request(open: Boolean) {
        if (!open) { focus.clearFocus(); keyboard?.hide() }
        pull.reset()
        state.moveTo(open, reducedNow, ::moved)
    }
    fun drag(delta: Float): Float {
        if (!state.dragging) { focus.clearFocus(); keyboard?.hide() }
        if (!externalOpen && !state.targetOpen) {
            if (!state.dragging && delta >= 0f) return 0f
            val consumed = pull.drag(delta, pullThreshold) { cue ->
                cueNow?.invoke(cue)
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            state.preview(pull.preview)
            return consumed
        }
        return state.drag(delta, travel)
    }
    fun dragHome(delta: Float): Float {
        var remaining = delta
        if (pull.active) remaining -= drag(remaining)
        if (preferences.layout == HomeLayout.LIST && remaining > 0 && homeScroll.value > 0) {
            remaining += homeScroll.dispatchRawDelta(-remaining)
        }
        remaining -= homeHeader.consume(remaining)
        if (preferences.layout == HomeLayout.LIST && remaining < 0) {
            remaining += homeScroll.dispatchRawDelta(-remaining)
        }
        if (remaining < 0f) remaining -= drag(remaining)
        return delta - remaining
    }
    fun finish(velocity: Float) {
        if (!state.dragging || state.settling) return
        if (!pointer.releasedNormally) { request(pointer.startedOpen); return }
        // Opening requires a deliberate distance. A fling can only help dismiss an already open drawer.
        request(if (!externalOpen && !state.targetOpen) pull.armed else drawerShouldOpen(state.progress, velocity, velocityThreshold))
    }
    LaunchedEffect(drawerOpen) {
        if (drawerOpen != state.targetOpen) {
            if (drawerOpen) request(true)
            else { focus.clearFocus(); keyboard?.hide(); pull.reset(); state.snapClosed() }
        }
    }
    var handledHome by remember { mutableIntStateOf(homeRequest) }
    LaunchedEffect(deviceLocked, unlockRequest) {
        if (deviceLocked || unlockRequest?.pending == true) {
            focus.clearFocus(); keyboard?.hide(); pull.reset(); state.snapClosed(); moved(false)
        }
    }
    LaunchedEffect(homeRequest) {
        if (handledHome != homeRequest) {
            handledHome = homeRequest; focus.clearFocus(); keyboard?.hide(); pull.reset(); state.snapClosed(); moved(false)
        }
    }
    val connection = remember(state, listState, homeHeader, homeScroll, pullThreshold, velocityThreshold) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (state.settling || lockedNow || reorderingNow) return Offset.Zero
                if (!externalOpen && !state.targetOpen && !state.dragging) {
                    if (available.y < 0 || homeScroll.value == 0) return Offset(0f, homeHeader.consume(available.y))
                }
                if (source == NestedScrollSource.UserInput && pointer.active &&
                    (state.dragging || (state.progress == 1f && !listState.canScrollBackward && available.y > 0))) {
                    return Offset(0f, drag(available.y))
                }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || !pointer.active || state.settling || lockedNow || reorderingNow) return Offset.Zero
                if (state.dragging || (state.progress == 0f && available.y < 0)) return Offset(0f, drag(available.y))
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!state.dragging) return Velocity.Zero
                finish(available.y)
                return available
            }
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!state.dragging) return Velocity.Zero
                finish(available.y)
                return available
            }
        }
    }
    val atHome by remember { derivedStateOf { !state.settling && !state.dragging && state.progress == 0f } }
    val atApps by remember { derivedStateOf { !state.settling && !state.dragging && state.progress == 1f } }
    val showDrawer by remember { derivedStateOf { state.progress > 0f || state.settling } }
    val homeReady = atHome && !deviceLocked && shortcutsReady
    val appsReady = atApps && !deviceLocked
    val homeDrag = rememberDraggableState { dragHome(it) }
    val headerDrag = rememberDraggableState { drag(it) }
    BackHandler(enabled = showDrawer && !appsReady) { request(false) }
    BoxWithConstraints(Modifier.fillMaxSize().testTag("home-apps-surface").nestedScroll(connection)
        .pointerInput(state) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pointer.active = true
                pointer.releasedNormally = false
                pointer.startedOpen = externalOpen
                try {
                    do {
                        // Observe termination before draggable/scrollable handles the same up event.
                        // Those APIs also call onDragStopped(0) for cancellation; zero is not a release.
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.none { it.pressed }) {
                            pointer.active = false
                            pointer.releasedNormally = event.type == PointerEventType.Release
                            break
                        }
                    } while (true)
                } finally {
                    pointer.active = false
                    if (!pointer.releasedNormally && state.dragging) request(pointer.startedOpen)
                }
            }
        }.semantics {
        this[AppsPullArmed] = pull.armed
        this[DrawerProgress] = state.progress
        this[DrawerSettling] = state.settling
        customActions = if (homeReady && !reordering) listOf(
            CustomAccessibilityAction("Open Apps") { request(true); true },
            CustomAccessibilityAction("Open nia settings") { onSettings(); true },
        ) else emptyList()
    }) {
        val height = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        SideEffect { if (travel == 1f || (homeReady && !deviceLocked)) travel = height }
        Box(Modifier.fillMaxSize().then(if (homeReady) Modifier else Modifier.clearAndSetSemantics {})
            .draggable(homeDrag, Orientation.Vertical,
                enabled = !reordering && shortcutsReady && !state.settling && !deviceLocked && !externalOpen,
                onDragStarted = { homeScroll.stopScroll() },
                onDragStopped = { finish(it) })) {
            HomeScreen(preferences.layout, time, date, onClock, onShortcut,
                textSize = preferences.homeTextSize, onSettings = onSettings, pixelClockLayout = preferences.pixelClockLayout,
                contentTopInset = contentTopInset, leftShortcut = preferences.leftShortcut, rightShortcut = preferences.rightShortcut,
                onCorner = onCorner, cornerLabel = { position ->
                    preferences.shortcut(position).command?.label ?: apps.find { it.target == preferences.shortcut(position).app }?.label ?: (position.label + " shortcut, unavailable")
                }, motionMode = preferences.motionMode, unlockRequest = unlockRequest, deviceLocked = deviceLocked,
                interactionEnabled = homeReady,
                onRevealReady = { shortcutsReady = it }, revealAllowed = revealAllowed,
                shortcuts = preferences.homeShortcuts, onDate = onDate, scroll = homeScroll,
                gesturesEnabled = !state.settling && !externalOpen && !deviceLocked && shortcutsReady,
                headerCollapse = homeHeader.fraction, onHeaderRange = homeHeader::updateRange,
                onReorder = onReorderHome, onReorderActive = { reordering = it }, onEditShortcut = onEditHome)
        }
        if (showDrawer || drawerOpen) {
            Box(Modifier.fillMaxSize().graphicsLayer {
                translationY = if (reduced) 0f else (1f - state.progress) * travel
                alpha = if (reduced) state.progress else (state.progress / 0.12f).coerceIn(0f, 1f)
            }.background(MaterialTheme.colorScheme.background)
                .then(if (appsReady) Modifier else Modifier.clearAndSetSemantics {})) {
                AppListScreen(apps, loading, preferences.searchAutofocus || searchAppsOnOpen, onBack = { if (appsReady) request(false) },
                    onSelect = { if (appsReady) onApp(it) }, onSettings = { if (appsReady) onSettings() }, onInfo = onAppInfo,
                    alphabetScrubber = preferences.alphabetScrubber, interactionEnabled = appsReady,
                    gesturesEnabled = !state.settling && !deviceLocked, listState = listState, onAssignCorner = onAssignCorner,
                    onAddHome = onAddHome, canAddHome = preferences.homeShortcuts.size < MAX_HOME_SHORTCUTS,
                    headerDragModifier = Modifier.draggable(headerDrag, Orientation.Vertical,
                        enabled = !state.settling && !deviceLocked, onDragStopped = { finish(it) }))
            }
        }
        if (pull.active) {
            val ink = MaterialTheme.colorScheme.onSurface
            Canvas(Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).size(24.dp)
                .graphicsLayer { alpha = (pull.distance / pullThreshold).coerceIn(0f, 1f) }.clearAndSetSemantics {}) {
                val stroke = 1.5.dp.toPx()
                drawLine(ink, Offset(size.width * 0.32f, size.height * 0.56f), Offset(size.width * 0.5f, size.height * 0.38f), stroke, StrokeCap.Round)
                drawLine(ink, Offset(size.width * 0.5f, size.height * 0.38f), Offset(size.width * 0.68f, size.height * 0.56f), stroke, StrokeCap.Round)
            }
        }
        if (state.settling || deviceLocked || !shortcutsReady) Box(Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        })
    }
}
