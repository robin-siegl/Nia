package dev.robinsiegl.nia

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w320dp-h600dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class SharedHomeGestureTest {
    private val scale = object : MotionDurationScale { override var scaleFactor by mutableFloatStateOf(1f) }
    @get:Rule val rule = createComposeRule(effectContext = scale)
    private var open by mutableStateOf(false)
    private var locked by mutableStateOf(false)
    private var homeRequest by mutableIntStateOf(0)
    private var unlock by mutableStateOf<UnlockRevealRequest?>(null)
    private val cues = mutableListOf<PullCue>()
    private val entries = (1..8).map { HomeShortcut("app-$it", "App $it", ShortcutIcon.STAR, AppTarget("example.app", ".Main$it")) }
    private fun render(layout: HomeLayout, populated: Boolean = false) {
        val p = LauncherPreferences(layout = layout, homeTextSize = HomeTextSize.LARGE, homeShortcuts = if (populated) entries else emptyList())
        rule.setContent { NiaTheme(Palette.generate(p, true)) {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                Surface(Modifier.fillMaxSize()) {
                    HomeAppsScreen(p, emptyList(), false, "14:03", "Fri, 30 Oct", open, { open = it }, {}, {}, {}, {}, {}, {},
                        homeRequest = homeRequest, unlockRequest = unlock, deviceLocked = locked, onPullCue = { cues += it })
                }
            }
        } }
        rule.mainClock.autoAdvance = false
    }
    private fun collapse() = rule.onNode(SemanticsMatcher.keyIsDefined(HomeHeaderCollapse), useUnmergedTree = true).fetchSemanticsNode().config[HomeHeaderCollapse]
    private fun progress() = rule.onNodeWithTag("home-apps-surface").fetchSemanticsNode().config[DrawerProgress]
    private fun armed() = rule.onNodeWithTag("home-apps-surface").fetchSemanticsNode().config[AppsPullArmed]
    private fun dragUpAndHold() {
        rule.onNodeWithTag("home-apps-surface").performTouchInput {
            down(Offset(width * 0.5f, height * 0.65f))
            moveTo(Offset(width * 0.5f, height * 0.15f), 400)
        }
    }
    private fun release() {
        rule.onNodeWithTag("home-apps-surface").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(500)
    }
    private fun assertArmedRelease() {
        dragUpAndHold()
        assertTrue(armed())
        assertFalse(open)
        assertTrue(progress() in 0.2f..0.6f)
        assertEquals(listOf(PullCue.ARMED), cues)
        release()
        assertTrue(open)
        assertEquals(1f, progress(), 0f)
    }
    @Test fun `grid uses resisted pull and opens only on armed release`() {
        render(HomeLayout.GRID, populated = true)
        assertArmedRelease()
    }
    @Test fun `Apps-only list uses the same pull cues and release threshold`() {
        render(HomeLayout.LIST)
        assertArmedRelease()
    }
    @Test fun `fast short grid fling does not arm or open Apps`() {
        render(HomeLayout.GRID)
        shortFling()
    }
    @Test fun `fast short list fling does not arm or open Apps`() {
        render(HomeLayout.LIST)
        shortFling()
    }
    private fun shortFling() {
        rule.onNodeWithTag("home-apps-surface").performTouchInput {
            down(Offset(width * 0.5f, height * 0.8f))
            moveTo(Offset(width * 0.5f, height * 0.77f), 5)
            moveTo(Offset(width * 0.5f, height * 0.73f), 5)
            up()
        }
        rule.mainClock.advanceTimeBy(500)
        assertFalse(open); assertEquals(0f, progress(), 0f)
        assertFalse(cues.contains(PullCue.ARMED))
    }
    @Test fun `long list scrolls before pulling and cancellation restores its previous presentation`() {
        render(HomeLayout.LIST, populated = true)
        rule.onNodeWithTag("home-list").performTouchInput { swipeUp(startY = height * 0.9f, endY = height * 0.6f, durationMillis = 300) }
        rule.mainClock.advanceTimeBy(500)
        assertFalse(open); assertTrue(cues.isEmpty())
        rule.mainClock.autoAdvance = true
        rule.onNodeWithTag("home-list").performTouchInput { swipeUp(startY = height * 0.9f, endY = height * 0.5f, durationMillis = 300) }
        rule.onNodeWithTag("home-entry-app-8").performScrollTo()
        rule.mainClock.autoAdvance = false
        val before = rule.onNodeWithTag("home-entry-app-8").fetchSemanticsNode().boundsInRoot
        dragUpAndHold()
        assertTrue(armed())
        rule.onNodeWithTag("home-apps-surface").performTouchInput { cancel() }
        rule.mainClock.advanceTimeBy(500)
        assertFalse(open); assertEquals(0f, progress(), 0f)
        assertEquals(before, rule.onNodeWithTag("home-entry-app-8").fetchSemanticsNode().boundsInRoot)
        rule.openAppsFromHome()
        rule.mainClock.advanceTimeBy(500)
        assertTrue(open)
        rule.onNodeWithText("Back").performClick()
        rule.mainClock.advanceTimeBy(500)
        assertEquals(before, rule.onNodeWithTag("home-entry-app-8").fetchSemanticsNode().boundsInRoot)
    }
    @Test fun `reversing an armed grid pull cancels and the next tap still works`() {
        render(HomeLayout.GRID)
        dragUpAndHold()
        assertTrue(armed())
        rule.onNodeWithTag("home-apps-surface").performTouchInput {
            moveTo(Offset(width * 0.5f, height * 0.65f), 300)
            up()
        }
        rule.mainClock.advanceTimeBy(500)
        assertFalse(open); assertEquals(0f, progress(), 0f)
        rule.openAppsFromHome()
        rule.mainClock.advanceTimeBy(500)
        assertTrue(open)
    }
    @Test fun `Android Home resets a scrolled list before its next visible frame`() {
        render(HomeLayout.LIST, populated = true)
        rule.mainClock.autoAdvance = true
        rule.onNodeWithTag("home-list").performTouchInput { swipeUp(startY = height * 0.9f, endY = height * 0.5f, durationMillis = 300) }
        rule.onNodeWithTag("home-entry-app-8").performScrollTo()
        rule.mainClock.autoAdvance = false
        assertTrue(collapse() > 0f)
        rule.runOnIdle { homeRequest++; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(32)
        assertEquals(0f, collapse(), 0f)
        rule.onNodeWithTag("home-clock").assertIsDisplayed()
    }
    @Test fun `locking with Apps open clears navigation and search before the next unlock`() {
        render(HomeLayout.LIST, populated = true)
        rule.openAppsFromHome()
        rule.mainClock.advanceTimeBy(500)
        assertTrue(open)
        rule.onNodeWithText("Search apps").performTextInput("old query")
        rule.runOnIdle { locked = true; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(32)
        assertFalse("Lock left navigation on a hidden Apps drawer", open)
        rule.runOnIdle { locked = false; unlock = UnlockRevealRequest(); Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithTag("home-clock").assertIsDisplayed()
        rule.openAppsFromHome()
        rule.mainClock.advanceTimeBy(500)
        assertTrue(open)
        rule.onNodeWithText("Clear").assertDoesNotExist()
    }
    @Test fun `Android Home also closes navigation after Apps has fully opened`() {
        render(HomeLayout.GRID)
        rule.openAppsFromHome()
        rule.mainClock.advanceTimeBy(500)
        assertTrue(open)
        rule.runOnIdle { homeRequest++; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(64)
        assertFalse("Android Home left the drawer's external state open", open)
        rule.openAppsFromHome()
        rule.mainClock.advanceTimeBy(500)
        assertTrue(open)
    }
    @Test fun `new unlock starts from an expanded header with disabled animations`() {
        scale.scaleFactor = 0f
        render(HomeLayout.LIST, populated = true)
        rule.mainClock.autoAdvance = true
        rule.onNodeWithTag("home-list").performTouchInput { swipeUp(startY = height * 0.9f, endY = height * 0.5f, durationMillis = 300) }
        rule.onNodeWithTag("home-entry-app-8").performScrollTo()
        rule.mainClock.autoAdvance = false
        assertTrue(collapse() > 0f)
        rule.runOnIdle { unlock = UnlockRevealRequest(); Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(64)
        assertEquals(0f, collapse(), 0f)
        rule.onNodeWithTag("home-clock").assertIsDisplayed()
        rule.openAppsFromHome()
        rule.mainClock.advanceTimeBy(64)
        assertTrue(open)
    }
}
