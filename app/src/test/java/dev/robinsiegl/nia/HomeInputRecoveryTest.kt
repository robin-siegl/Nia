package dev.robinsiegl.nia

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
@Config(sdk = [35], qualifiers = "w412dp-h892dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class HomeInputRecoveryTest {
    private val scale = object : MotionDurationScale {
        override var scaleFactor by mutableFloatStateOf(1f)
    }
    @get:Rule val rule = createComposeRule(effectContext = scale)
    private var locked by mutableStateOf(false)
    private var unlock by mutableStateOf<UnlockRevealRequest?>(null)
    private var open by mutableStateOf(false)
    private var active by mutableStateOf(true)
    private var categories = 0
    private var clocks = 0
    private var corners = 0
    private var music = Offset.Zero
    private var clock = Offset.Zero
    private var corner = Offset.Zero
    private var appsButton = Offset.Zero
    private val app = AppEntry(AppTarget("example.notes", ".Main"), "Notes")

    private fun render(layout: HomeLayout = HomeLayout.GRID, motion: MotionMode = MotionMode.SYSTEM) {
        val p = LauncherPreferences(layout = layout, themeMode = ThemeMode.DARK, motionMode = motion,
            leftShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.NOTES, app.target))
        rule.setContent {
            NiaTheme(Palette.generate(p, true)) {
                Surface(Modifier.fillMaxSize()) {
                    // A fixed clock and plain callback counters must not accidentally refresh Home.
                    HomeAppsScreen(p.copy(homeShortcuts = legacyHomeShortcuts()), listOf(app), false, "14:03", "Fri, 30 Oct", open, { open = it },
                        { clocks++ }, { categories++ }, {}, {}, {}, { corners++ },
                        unlockRequest = unlock, deviceLocked = locked, revealAllowed = active)
                }
            }
        }
        music = (if (layout == HomeLayout.GRID) rule.onNodeWithContentDescription("Music")
            else rule.onNodeWithTag("home-entry-Music")).fetchSemanticsNode().boundsInRoot.center
        appsButton = (if (layout == HomeLayout.GRID) rule.onNodeWithContentDescription("Apps")
            else rule.onNodeWithTag("home-entry-Apps")).fetchSemanticsNode().boundsInRoot.center
        clock = rule.onNodeWithTag("home-clock").fetchSemanticsNode().boundsInRoot.center
        corner = rule.onNodeWithTag("home-corner-LEFT").fetchSemanticsNode().boundsInRoot.center
        rule.mainClock.autoAdvance = false
    }

    private fun beginUnlock() {
        rule.runOnIdle { locked = true; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(32)
        rule.runOnIdle { locked = false; unlock = UnlockRevealRequest(); Snapshot.sendApplyNotifications() }
    }

    private fun assertHomeResponds() {
        // Real pointer events, not semantics actions, must pass the transition blocker.
        rule.onNodeWithTag("home-apps-surface").performTouchInput { click(music) }
        assertEquals("Home stayed blocked after the reveal finished", 1, categories)
        rule.onNodeWithTag("home-apps-surface").performTouchInput { click(clock) }
        assertEquals(1, clocks)
        rule.onNodeWithTag("home-apps-surface").performTouchInput { click(corner) }
        assertEquals(1, corners)
        rule.onNodeWithTag("home-apps-surface").performTouchInput { click(appsButton) }
        rule.mainClock.advanceTimeBy(500)
        assertTrue("Apps tap stayed blocked", open)
        rule.onNodeWithText("Back").performClick()
        rule.mainClock.advanceTimeBy(500)
        assertFalse(open)
        rule.onNodeWithTag("home-apps-surface").performTouchInput { swipeUp() }
        rule.mainClock.advanceTimeBy(500)
        assertTrue("Swipe to Apps stayed blocked", open)
    }

    @Test fun `grid unlock restores all input without a clock tick or other refresh`() {
        render()
        beginUnlock()
        rule.mainClock.advanceTimeBy(1_000)
        assertHomeResponds()
    }

    @Test fun `list unlock restores all input without a clock tick or other refresh`() {
        render(layout = HomeLayout.LIST)
        beginUnlock()
        rule.mainClock.advanceTimeBy(1_000)
        assertHomeResponds()
    }

    @Test fun `reduced motion unlock also releases the Home blocker`() {
        render(motion = MotionMode.REDUCED)
        beginUnlock()
        rule.mainClock.advanceTimeBy(300)
        assertHomeResponds()
    }

    @Test fun `disabled system animations release Home input after unlock`() {
        scale.scaleFactor = 0f
        render()
        beginUnlock()
        rule.mainClock.advanceTimeBy(64)
        assertHomeResponds()
    }

    @Test fun `disabling animations during the reveal releases input immediately`() {
        render()
        beginUnlock()
        rule.mainClock.advanceTimeBy(240)
        rule.runOnIdle { scale.scaleFactor = 0f; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(64)
        assertHomeResponds()
    }

    @Test fun `cancelled Home swipe settles and accepts the next gesture`() {
        render()
        rule.onNodeWithTag("home-apps-surface").performTouchInput {
            down(Offset(width / 2f, height * 0.9f))
            moveTo(Offset(width / 2f, height * 0.7f), 200)
            cancel()
        }
        rule.mainClock.advanceTimeBy(500)
        assertHomeResponds()
    }

    @Test fun `changing animation scale during drawer open and close does not trap input`() {
        render()
        rule.onNodeWithTag("home-apps-surface").performTouchInput { click(appsButton) }
        rule.mainClock.advanceTimeBy(64)
        assertFalse(open)
        rule.runOnIdle { scale.scaleFactor = 0f; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(64)
        assertTrue(open)
        rule.runOnIdle { scale.scaleFactor = 1f; Snapshot.sendApplyNotifications() }
        rule.onNodeWithText("Back").performClick()
        rule.mainClock.advanceTimeBy(64)
        assertTrue(open)
        rule.runOnIdle { scale.scaleFactor = 0f; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(64)
        assertFalse(open)
        rule.runOnIdle { scale.scaleFactor = 1f; Snapshot.sendApplyNotifications() }
        assertHomeResponds()
    }

    @Test fun `unlock stays pending until the Home window can show it`() {
        render()
        rule.runOnIdle { active = false; Snapshot.sendApplyNotifications() }
        beginUnlock()
        rule.mainClock.advanceTimeBy(2_000)
        assertTrue("An unfocused window consumed the reveal", unlock!!.pending)
        assertEquals(0, categories)
        rule.runOnIdle { active = true; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(64)
        assertTrue("Reveal was marked handled before finishing", unlock!!.pending)
        rule.mainClock.advanceTimeBy(1_000)
        assertFalse(unlock!!.pending)
        assertHomeResponds()
    }

    @Test fun `a focus interruption retains the reveal until a visible completion`() {
        render()
        beginUnlock()
        rule.mainClock.advanceTimeBy(240)
        assertTrue("In-progress reveal was consumed too early", unlock!!.pending)
        rule.runOnIdle { active = false; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(2_000)
        assertTrue(unlock!!.pending)
        rule.runOnIdle { active = true; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(64)
        assertTrue(unlock!!.pending)
        rule.mainClock.advanceTimeBy(1_000)
        assertFalse(unlock!!.pending)
        rule.runOnIdle { active = false; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(32)
        rule.runOnIdle { active = true; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(32)
        assertHomeResponds()
    }

    @Test fun `a retained replacement restarts when pause and resume happen between frames`() {
        render()
        beginUnlock()
        rule.mainClock.advanceTimeBy(240)
        val previous = unlock!!
        rule.runOnIdle {
            active = false
            unlock = UnlockRevealRequest()
            active = true
            previous.consume()
            Snapshot.sendApplyNotifications()
        }
        rule.mainClock.advanceTimeBy(64)
        assertTrue("Old completion consumed a replacement reveal", unlock!!.pending)
        rule.mainClock.advanceTimeBy(1_000)
        assertFalse(unlock!!.pending)
        assertHomeResponds()
    }
}
