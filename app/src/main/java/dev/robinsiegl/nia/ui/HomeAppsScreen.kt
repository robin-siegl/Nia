package dev.robinsiegl.nia.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import dev.robinsiegl.nia.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

internal val DrawerProgress = SemanticsPropertyKey<Float>("DrawerProgress")
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
    onClock: () -> Unit, onCategory: (HomeCategory) -> Unit, onSettings: () -> Unit,
    onApp: (AppEntry) -> Unit, onAppInfo: (AppEntry) -> Unit, onCorner: (CornerPosition) -> Unit,
    contentTopInset: Dp = 0.dp, unlockRequest: UnlockRevealRequest? = null, deviceLocked: Boolean = false, homeRequest: Int = 0,
    onAssignCorner: ((AppEntry, CornerPosition) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val state = remember { DrawerMotion(drawerOpen, scope) }
    val externalOpen by rememberUpdatedState(drawerOpen)
    val onChanged by rememberUpdatedState(onDrawerOpenChanged)
    val reduced = preferences.motionMode == MotionMode.REDUCED
    val reducedNow by rememberUpdatedState(reduced)
    val lockedNow by rememberUpdatedState(deviceLocked)
    val density = LocalDensity.current
    val velocityThreshold = with(density) { 700.dp.toPx() }
    val listState = rememberLazyListState()
    var homeScrollable by remember { mutableStateOf(false) }
    var shortcutsReady by remember { mutableStateOf(!deviceLocked) }
    var travel by remember { mutableFloatStateOf(1f) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    fun moved(open: Boolean) { if (externalOpen != open) onChanged(open) }
    fun request(open: Boolean) {
        if (!open) { focus.clearFocus(); keyboard?.hide() }
        state.moveTo(open, reducedNow, ::moved)
    }
    fun drag(delta: Float): Float {
        if (!state.dragging) { focus.clearFocus(); keyboard?.hide() }
        return state.drag(delta, travel)
    }
    fun finish(velocity: Float) { request(drawerShouldOpen(state.progress, velocity, velocityThreshold)) }
    LaunchedEffect(drawerOpen) {
        if (drawerOpen != state.targetOpen) {
            if (drawerOpen) request(true)
            else { focus.clearFocus(); keyboard?.hide(); state.snapClosed() }
        }
    }
    var handledHome by remember { mutableIntStateOf(homeRequest) }
    LaunchedEffect(homeRequest) {
        if (handledHome != homeRequest) {
            handledHome = homeRequest; focus.clearFocus(); keyboard?.hide(); state.snapClosed()
        }
    }
    val connection = remember(state, listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || state.settling || lockedNow) return Offset.Zero
                if (state.dragging || (state.progress == 1f && !listState.canScrollBackward && available.y > 0)) {
                    return Offset(0f, drag(available.y))
                }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || state.settling || lockedNow) return Offset.Zero
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
    val busy = state.settling || state.dragging
    val atHome by remember { derivedStateOf { !state.settling && !state.dragging && state.progress == 0f } }
    val atApps by remember { derivedStateOf { !state.settling && !state.dragging && state.progress == 1f } }
    val showDrawer by remember { derivedStateOf { state.progress > 0f || state.settling } }
    val homeReady = atHome && !deviceLocked && shortcutsReady
    val appsReady = atApps && !deviceLocked
    val homeDrag = rememberDraggableState { drag(it) }
    val headerDrag = rememberDraggableState { drag(it) }
    BackHandler(enabled = showDrawer && !appsReady) { /* Finish this transition before accepting another. */ }
    BoxWithConstraints(Modifier.fillMaxSize().testTag("home-apps-surface").nestedScroll(connection).semantics {
        this[DrawerProgress] = state.progress
        this[DrawerSettling] = state.settling
    }) {
        val height = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        SideEffect { if (travel == 1f || (homeReady && !deviceLocked)) travel = height }
        Box(Modifier.fillMaxSize().then(if (homeReady) Modifier else Modifier.clearAndSetSemantics {})
            .draggable(homeDrag, Orientation.Vertical,
                enabled = !homeScrollable && shortcutsReady && !state.settling && !deviceLocked && !externalOpen,
                onDragStopped = { finish(it) })) {
            HomeScreen(preferences.layout, time, date, onClock, onCategory, { request(true) },
                textSize = preferences.homeTextSize, onSettings = onSettings, pixelClockLayout = preferences.pixelClockLayout,
                contentTopInset = contentTopInset, leftShortcut = preferences.leftShortcut, rightShortcut = preferences.rightShortcut,
                onCorner = onCorner, cornerLabel = { position ->
                    apps.find { it.target == preferences.shortcut(position).app }?.label ?: (position.label + " shortcut, unavailable")
                }, motionMode = preferences.motionMode, unlockRequest = unlockRequest, deviceLocked = deviceLocked,
                interactionEnabled = homeReady, onScrollable = { homeScrollable = it },
                onRevealReady = { shortcutsReady = it })
        }
        if (showDrawer || drawerOpen) {
            Box(Modifier.fillMaxSize().graphicsLayer {
                translationY = if (reduced) 0f else (1f - state.progress) * travel
                alpha = if (reduced) state.progress else (state.progress / 0.12f).coerceIn(0f, 1f)
            }.background(MaterialTheme.colorScheme.background)
                .then(if (appsReady) Modifier else Modifier.clearAndSetSemantics {})) {
                AppListScreen(apps, loading, preferences.searchAutofocus, onBack = { if (appsReady) request(false) },
                    onSelect = { if (appsReady) onApp(it) }, onSettings = { if (appsReady) onSettings() }, onInfo = onAppInfo,
                    alphabetScrubber = preferences.alphabetScrubber, interactionEnabled = appsReady,
                    gesturesEnabled = !state.settling && !deviceLocked, listState = listState, onAssignCorner = onAssignCorner,
                    headerDragModifier = Modifier.draggable(headerDrag, Orientation.Vertical,
                        enabled = !state.settling && !deviceLocked, onDragStopped = { finish(it) }))
            }
        }
        if (busy || deviceLocked || !shortcutsReady) Box(Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        })
    }
}
