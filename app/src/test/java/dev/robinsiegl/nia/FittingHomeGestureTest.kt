package dev.robinsiegl.nia

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dev.robinsiegl.nia.model.*
import dev.robinsiegl.nia.theme.Palette
import dev.robinsiegl.nia.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h892dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class FittingHomeGestureTest {
    private val scale = object : MotionDurationScale { override var scaleFactor by mutableFloatStateOf(1f) }
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>(effectContext = scale)
    private lateinit var host: android.view.View
    private var p by mutableStateOf(LauncherPreferences())
    private var open by mutableStateOf(false)
    private var homeRequest by mutableIntStateOf(0)
    private var launches = 0
    private val cues = mutableListOf<PullCue>()
    private val entries = (1..9).map { HomeShortcut("app-$it", "App $it", ShortcutIcon.STAR, AppTarget("example.app", ".Main$it")) }
    private var start = Offset.Zero

    @Suppress("DEPRECATION")
    private fun render(layout: HomeLayout, count: Int = 3, large: Boolean = false, light: Boolean = false,
        motion: MotionMode = MotionMode.SYSTEM) {
        if (large) rule.activity.resources.let { it.updateConfiguration(Configuration(it.configuration).apply { fontScale = 2f }, it.displayMetrics) }
        p = LauncherPreferences(layout = layout, homeShortcuts = entries.take(count),
            homeTextSize = if (large) HomeTextSize.LARGE else HomeTextSize.DEFAULT,
            themeMode = if (light) ThemeMode.LIGHT else ThemeMode.DARK, motionMode = motion)
        rule.setContent {
            host = LocalView.current
            NiaTheme(Palette.generate(p, false)) { Surface(Modifier.fillMaxSize()) {
                HomeAppsScreen(p, emptyList(), false, "14:03", "Fri, 30 Oct", open, { open = it }, {}, { launches++ }, {}, {}, {}, {},
                    homeRequest = homeRequest, onPullCue = { cues += it })
            } }
        }
        rule.mainClock.autoAdvance = false
    }
    private fun root() = rule.onNodeWithTag("home-apps-surface")
    private fun progress() = root().fetchSemanticsNode().config[DrawerProgress]
    private fun header() = root().fetchSemanticsNode().config[HomeHeaderProgress]
    private fun entry() = rule.onNodeWithTag("home-entry-app-1")
    private fun listMaximum() = rule.onNodeWithTag("home-list").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].maxValue()
    private fun capture(name: String) = rule.runOnIdle {
        val bitmap = Bitmap.createBitmap(host.width, host.height, Bitmap.Config.ARGB_8888)
        host.draw(Canvas(bitmap))
        File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    private fun partial(fromList: Boolean = p.layout == HomeLayout.LIST) {
        val bounds = root().fetchSemanticsNode().boundsInRoot
        start = if (fromList) rule.onNodeWithTag("home-entry-app-2").fetchSemanticsNode().boundsInRoot.center
            else Offset(bounds.width / 2f, bounds.height * 0.78f)
        root().performTouchInput { down(start); moveBy(Offset(0f, -80f), 200) }
        rule.mainClock.advanceTimeBy(32)
    }
    private fun assertConcurrent() {
        assertFalse(open)
        assertTrue("Apps waited for header compaction", progress() > 0f)
        assertTrue("Header did not morph alongside Apps", header() > 0f && header() < 1f)
        assertTrue(cues.isEmpty())
    }
    private fun assertReversal(layout: HomeLayout, large: Boolean = false, light: Boolean = false) {
        render(layout, count = if (layout == HomeLayout.GRID) 9 else 3, large = large, light = light)
        val first = entry().fetchSemanticsNode().boundsInRoot
        val clock = rule.onNodeWithTag("home-clock").fetchSemanticsNode().boundsInRoot
        if (layout == HomeLayout.LIST) assertEquals(0f, listMaximum(), 0f)
        partial()
        assertConcurrent()
        capture("home-fit-${layout.name.lowercase()}-pull")
        val before = progress()
        root().performTouchInput { moveBy(Offset(0f, -40f), 100) }
        rule.mainClock.advanceTimeBy(32)
        assertTrue(progress() > before)
        root().performTouchInput { moveTo(start, 200); up() }
        rule.mainClock.advanceTimeBy(500)
        assertFalse(open); assertEquals(0f, progress(), 0f); assertEquals(0f, header(), 0f)
        assertEquals(first, entry().fetchSemanticsNode().boundsInRoot)
        assertEquals(clock, rule.onNodeWithTag("home-clock").fetchSemanticsNode().boundsInRoot)
        entry().performTouchInput { click() }
        assertEquals(1, launches)
        assertTrue(cues.isEmpty())
    }
    @Test fun `a fitting grid reveals Apps immediately while morphing and reverses without moving Home`() = assertReversal(HomeLayout.GRID)
    @Test fun `a fitting list does the same through child scrolling at large text in light mode`() =
        assertReversal(HomeLayout.LIST, large = true, light = true)

    private fun assertOpen(layout: HomeLayout, large: Boolean = false, light: Boolean = false,
        motion: MotionMode = MotionMode.SYSTEM) {
        render(layout, large = large, light = light, motion = motion)
        val first = entry().fetchSemanticsNode().boundsInRoot
        val clock = rule.onNodeWithTag("home-clock").fetchSemanticsNode().boundsInRoot
        partial(); assertConcurrent()
        root().performTouchInput { moveBy(Offset(0f, -240f), 200) }
        rule.mainClock.advanceTimeBy(32)
        assertEquals(1f, header(), 0.001f)
        assertEquals(listOf(PullCue.ARMED), cues)
        assertFalse(open)
        root().performTouchInput { up() }
        rule.mainClock.advanceTimeBy(500)
        assertTrue(open); assertEquals(1f, progress(), 0f); assertEquals(1f, header(), 0f)
        rule.onNodeWithText("Back").performClick()
        if (scale.scaleFactor > 0f) {
            rule.mainClock.advanceTimeBy(if (motion == MotionMode.REDUCED) 64 else 128)
            assertTrue("Header did not expand with drawer dismissal", header() > 0f && header() < 1f)
            capture("home-fit-${layout.name.lowercase()}-return${if (light) "-light" else ""}${if (motion == MotionMode.REDUCED) "-reduced" else ""}")
        }
        rule.mainClock.advanceTimeBy(500)
        assertFalse(open)
        assertEquals(0f, header(), 0f)
        assertEquals(first, entry().fetchSemanticsNode().boundsInRoot)
        assertEquals(clock, rule.onNodeWithTag("home-clock").fetchSemanticsNode().boundsInRoot)
        rule.onAllNodesWithTag("home-clock").assertCountEquals(1)
        rule.onAllNodesWithTag("home-date").assertCountEquals(1)
        entry().performTouchInput { click() }
        assertEquals(1, launches)
    }
    @Test fun `returning from Apps re-expands a fitting grid and restores its original positions`() = assertOpen(HomeLayout.GRID)
    @Test fun `returning from Apps re-expands a fitting list and restores its original positions`() = assertOpen(HomeLayout.LIST)
    @Test fun `returning restores the large-text fitting list in a light theme`() = assertOpen(HomeLayout.LIST, large = true, light = true)
    @Test fun `reduced motion re-expands fitting Home and releases input`() = assertOpen(HomeLayout.GRID, motion = MotionMode.REDUCED)
    @Test fun `disabled animations restore fitting Home immediately after returning`() {
        scale.scaleFactor = 0f
        assertOpen(HomeLayout.LIST)
    }

    private fun dismissBySwipe(layout: HomeLayout, cancelDismissal: Boolean) {
        render(layout)
        val first = entry().fetchSemanticsNode().boundsInRoot
        root().performTouchInput { swipeUp() }
        rule.mainClock.advanceTimeBy(500)
        assertTrue(open); assertEquals(1f, header(), 0f)
        val title = rule.onNodeWithText("Apps").fetchSemanticsNode().boundsInRoot.center
        val height = root().fetchSemanticsNode().boundsInRoot.height
        root().performTouchInput { down(title); moveBy(Offset(0f, height * 0.85f), 400) }
        rule.mainClock.advanceTimeBy(32)
        assertTrue("Header stayed compact during swipe-down dismissal", header() > 0f && header() < 1f)
        if (cancelDismissal) {
            root().performTouchInput { cancel() }
            rule.mainClock.advanceTimeBy(500)
            assertTrue(open); assertEquals(1f, header(), 0f)
            rule.onNodeWithText("Back").performClick()
        } else root().performTouchInput { up() }
        rule.mainClock.advanceTimeBy(500)
        assertFalse(open); assertEquals(0f, header(), 0f)
        assertEquals(first, entry().fetchSemanticsNode().boundsInRoot)
        assertEquals(listOf(PullCue.ARMED), cues)
        entry().performTouchInput { click() }; assertEquals(1, launches)
    }
    @Test fun `swiping Apps down re-expands fitting Home with the same transition`() = dismissBySwipe(HomeLayout.GRID, cancelDismissal = false)
    @Test fun `canceling a dismissal keeps the list header coupled for the next return`() = dismissBySwipe(HomeLayout.LIST, cancelDismissal = true)

    @Test fun `an empty Home re-expands and can open Apps again without a clock tick`() {
        render(HomeLayout.LIST, count = 0)
        val clock = rule.onNodeWithTag("home-clock").fetchSemanticsNode().boundsInRoot
        root().performTouchInput { swipeUp() }
        rule.mainClock.advanceTimeBy(500)
        assertTrue(open)
        rule.onNodeWithText("Back").performClick()
        rule.mainClock.advanceTimeBy(500)
        assertEquals(0f, header(), 0f)
        assertEquals(clock, rule.onNodeWithTag("home-clock").fetchSemanticsNode().boundsInRoot)
        rule.openAppsFromHome()
        rule.mainClock.advanceTimeBy(500)
        assertTrue(open)
    }

    @Test fun `canceling an armed grid pull restores the header through intermediate animation frames`() {
        render(HomeLayout.GRID)
        partial()
        root().performTouchInput { moveBy(Offset(0f, -240f), 200); cancel() }
        rule.mainClock.advanceTimeBy(48)
        assertFalse(open)
        assertTrue("Header snapped instead of following drawer cancellation", header() > 0f && header() < 1f)
        capture("home-fit-grid-cancel")
        rule.mainClock.advanceTimeBy(500)
        assertEquals(0f, header(), 0f); assertEquals(0f, progress(), 0f)
        entry().performTouchInput { click() }; assertEquals(1, launches)
    }
    @Test fun `reduced motion preserves fitting list preview cancellation and immediate taps`() {
        render(HomeLayout.LIST, motion = MotionMode.REDUCED)
        partial(); assertConcurrent()
        root().performTouchInput { cancel() }
        rule.mainClock.advanceTimeBy(300)
        assertEquals(0f, header(), 0f); assertEquals(0f, progress(), 0f)
        entry().performTouchInput { click() }; assertEquals(1, launches)
    }
    @Test fun `disabled animations still preview the gesture and release all input after cancellation`() {
        scale.scaleFactor = 0f
        render(HomeLayout.GRID)
        partial(); assertConcurrent()
        root().performTouchInput { cancel() }
        rule.mainClock.advanceTimeBy(64)
        assertEquals(0f, header(), 0f); assertEquals(0f, progress(), 0f)
        entry().performTouchInput { click() }; assertEquals(1, launches)
        root().performTouchInput { swipeUp() }
        rule.mainClock.advanceTimeBy(64)
        assertTrue(open)
    }
    @Test @Config(qualifiers = "w412dp-h600dp-xhdpi")
    fun `an overflowing list keeps its sequence even when contraction makes the remaining content fit`() {
        render(HomeLayout.LIST, count = 5)
        assertTrue("The fixture must initially overflow", listMaximum() > 0f)
        partial()
        val remainingOverflow = listMaximum()
        root().performTouchInput { moveBy(Offset(0f, -remainingOverflow - 8f), 100) }
        rule.mainClock.advanceTimeBy(32)
        assertEquals("Header contraction should remove this small overflow", 0f, listMaximum(), 0f)
        assertEquals(0f, progress(), 0f)
        val before = header()
        assertTrue(before > 0f && before < 1f)
        root().performTouchInput { moveBy(Offset(0f, -40f), 100) }
        rule.mainClock.advanceTimeBy(32)
        assertEquals("Gesture route changed halfway through", 0f, progress(), 0f)
        assertTrue(header() > before && header() < 1f)
        root().performTouchInput { cancel() }
        rule.mainClock.advanceTimeBy(500)
        val previousHeader = header()
        // A fresh gesture can use the now-fitting viewport without replaying a header-only stage.
        partial()
        assertTrue(progress() > 0f)
        assertTrue(header() > previousHeader)
        root().performTouchInput { cancel() }
        rule.mainClock.advanceTimeBy(500)
        assertEquals(previousHeader, header(), 0f)
    }
    @Test fun `layout changes and fresh Home requests use the latest viewport for the next swipe`() {
        render(HomeLayout.LIST, count = 9, large = true)
        assertTrue(listMaximum() > 0f)
        rule.runOnIdle { p = p.copy(layout = HomeLayout.GRID); homeRequest++; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(64)
        partial(fromList = false); assertConcurrent()
        rule.runOnIdle { homeRequest++; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(64)
        root().performTouchInput { cancel() }
        rule.mainClock.advanceTimeBy(500)
        assertEquals(0f, header(), 0f); assertEquals(0f, progress(), 0f)
        rule.runOnIdle { p = p.copy(layout = HomeLayout.LIST, homeShortcuts = entries.take(3)); homeRequest++; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(64)
        assertEquals(0f, listMaximum(), 0f)
        partial(); assertConcurrent()
        root().performTouchInput { cancel() }
        rule.mainClock.advanceTimeBy(500)
        entry().performTouchInput { click() }; assertEquals(1, launches)
    }
}
