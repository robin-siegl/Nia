package dev.robinsiegl.nia

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalView
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h892dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class QolScreensTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var host: android.view.View
    private val apps = (0..50).map { AppEntry(AppTarget("example.app", ".App$it"), "App " + it.toString().padStart(2, '0')) }
    private fun render(p: LauncherPreferences = LauncherPreferences(), content: @Composable () -> Unit) {
        rule.setContent {
            host = LocalView.current
            NiaTheme(Palette.generate(p.copy(themeMode = ThemeMode.DARK), true)) { Surface(Modifier.fillMaxSize(), content = content) }
        }
    }
    @Composable
    private fun drawer(p: LauncherPreferences, open: Boolean, onOpen: (Boolean) -> Unit, onApp: (AppEntry) -> Unit = {}) {
        HomeAppsScreen(p.copy(homeShortcuts = legacyHomeShortcuts()), apps, false, "14:03", "Fri, 30 Oct", open, onOpen, {}, {}, {}, onApp, {}, {})
    }
    private fun progress() = rule.onNodeWithTag("home-apps-surface").fetchSemanticsNode().config[DrawerProgress]
    private fun capture(name: String) = rule.runOnIdle {
        val bitmap = android.graphics.Bitmap.createBitmap(host.width, host.height, android.graphics.Bitmap.Config.ARGB_8888)
        host.draw(android.graphics.Canvas(bitmap))
        File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }.outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun `tap opens with delayed focus and closes through the same animated state`() {
        var open by mutableStateOf(false)
        var launches = 0
        val p = LauncherPreferences(searchAutofocus = true)
        render { drawer(p, open, { open = it }, { launches++ }) }
        val clockId = rule.onNodeWithTag("pixel-clock-header").fetchSemanticsNode().id
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("Apps").performClick()
        rule.mainClock.advanceTimeBy(80)
        assertTrue(progress() in 0.01f..0.99f)
        assertTrue(rule.onNodeWithTag("home-apps-surface").fetchSemanticsNode().config[DrawerSettling])
        assertFalse(open)
        rule.onRoot().performTouchInput { click(Offset(width * 0.4f, height * 0.5f)) }
        assertEquals(0, launches)
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("Search apps").assertIsFocused()
        assertTrue(open)
        rule.onNodeWithText("Back").performClick()
        rule.mainClock.advanceTimeBy(500)
        assertFalse(open)
        assertEquals(0f, progress(), 0f)
        assertEquals(clockId, rule.onNodeWithTag("pixel-clock-header").fetchSemanticsNode().id)
    }

    @Test fun `drawer follows a partial finger drag and a reversed gesture cancels it`() {
        var open by mutableStateOf(false)
        render { drawer(LauncherPreferences(), open, { open = it }) }
        rule.onNodeWithTag("home-apps-surface").performTouchInput {
            down(Offset(width / 2f, height * 0.9f))
            moveTo(Offset(width / 2f, height * 0.6f), 200)
        }
        assertTrue("Drawer progress was " + progress(), progress() in 0.3f..0.55f)
        assertFalse(open)
        rule.onNodeWithTag("home-apps-surface").performTouchInput {
            moveTo(Offset(width / 2f, height * 0.9f), 250)
            up()
        }
        rule.waitForIdle()
        assertEquals(0f, progress(), 0f)
        assertFalse(open)
        rule.onNodeWithContentDescription("Apps").assertIsDisplayed()
    }

    @Test fun `scrolling Apps does not dismiss it until the list is at its top`() {
        var open by mutableStateOf(true)
        render { drawer(LauncherPreferences(), open, { open = it }) }
        rule.onNodeWithTag("apps-list").performScrollToIndex(30)
        rule.onNodeWithTag("apps-list").performTouchInput { swipeDown(endY = height * 0.5f) }
        rule.waitForIdle()
        assertTrue(open)
        assertEquals(1f, progress(), 0f)
        rule.onNodeWithTag("apps-list").performScrollToIndex(0)
        rule.onNodeWithTag("apps-list").performTouchInput { swipeDown() }
        rule.waitForIdle()
        assertFalse(open)
        assertEquals(0f, progress(), 0f)
    }

    @Test fun `an empty drawer can be pulled closed too`() {
        var open by mutableStateOf(true)
        render {
            HomeAppsScreen(LauncherPreferences(homeShortcuts = legacyHomeShortcuts()), emptyList(), false, "14:03", "Fri, 30 Oct", open, { open = it }, {}, {}, {}, {}, {}, {})
        }
        rule.onNodeWithTag("apps-screen").performTouchInput { swipeDown() }
        rule.waitForIdle()
        assertFalse(open)
    }

    @Test fun `launching an app prepares Home without replaying a drawer close on return`() {
        var open by mutableStateOf(true)
        render { drawer(LauncherPreferences(), open, { open = it }, { open = false }) }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("App 00").performClick()
        rule.runOnIdle { Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(32)
        assertEquals(0f, progress(), 0f)
        assertFalse(rule.onNodeWithTag("home-apps-surface").fetchSemanticsNode().config[DrawerSettling])
        rule.onNodeWithContentDescription("Apps").assertIsDisplayed()
    }

    @Test fun `Android Home interrupts an opening drawer without leaving input blocked`() {
        var open by mutableStateOf(false)
        var homeRequest by mutableIntStateOf(0)
        render {
            HomeAppsScreen(LauncherPreferences(homeShortcuts = legacyHomeShortcuts()), apps, false, "14:03", "Fri, 30 Oct", open, { open = it },
                {}, {}, {}, {}, {}, {}, homeRequest = homeRequest)
        }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("Apps").performClick()
        rule.mainClock.advanceTimeBy(64)
        rule.runOnIdle { homeRequest++ }
        rule.mainClock.advanceTimeBy(32)
        assertEquals(0f, progress(), 0f)
        assertFalse(open)
        rule.onNodeWithContentDescription("Music").assertIsEnabled()
    }

    @Test fun `Appearance offers a persistent reduced motion choice`() {
        var p by mutableStateOf(LauncherPreferences())
        render {
            SettingsDetailScreen(SettingsSection.APPEARANCE, p, emptyList(), false, {}, { p = it(p) },
                {}, {}, {}, {}, {}, {}, {})
        }
        rule.onNodeWithText("Reduced").performClick()
        assertEquals(MotionMode.REDUCED, p.motionMode)
        rule.onNodeWithText("Standard").performClick()
        assertEquals(MotionMode.SYSTEM, p.motionMode)
    }

    @Test fun `search focus waits for readiness and Enter only launches a sole match`() {
        var ready by mutableStateOf(false)
        val launches = mutableListOf<String>()
        render { AppListScreen(apps, false, true, onBack = {}, onSelect = { launches += it.label }, interactionEnabled = ready) }
        rule.onNodeWithText("Search apps").assertIsNotFocused()
        rule.runOnIdle { ready = true }
        rule.onNodeWithText("Search apps").assertIsFocused()
        rule.onNodeWithText("Search apps").performTextInput("App")
        rule.onNodeWithText("Search apps").performImeAction()
        assertTrue(launches.isEmpty())
        rule.onNodeWithText("Search apps").performTextReplacement("App 42")
        rule.onNodeWithText("Search apps").performImeAction()
        assertEquals(listOf("App 42"), launches)
    }

    @Test fun `physical Enter also launches the sole result once`() {
        var launches = 0
        render { AppListScreen(apps, false, false, onBack = {}, onSelect = { launches++ }) }
        rule.onNodeWithText("Search apps").performTextInput("App 42")
        rule.onNodeWithText("Search apps").performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, launches)
    }

    @Test fun `empty corners stay absent and long pressing blank Home opens settings`() {
        var settings = 0
        render { HomeScreen(HomeLayout.GRID, "14:03", "Fri, 30 Oct", {}, {}, {}, onSettings = { settings++ }, shortcuts = legacyHomeShortcuts()) }
        rule.onNodeWithTag("home-corner-LEFT").assertDoesNotExist()
        rule.onNodeWithTag("home-corner-RIGHT").assertDoesNotExist()
        rule.onNodeWithTag("home-screen").performTouchInput { longClick(Offset(width / 2f, height * 0.85f)) }
        assertEquals(1, settings)
    }

    @Test fun `both configurable corners retain their app labels and actions`() {
        var chosen: CornerPosition? = null
        val left = CornerShortcut(ShortcutAction.APP, ShortcutIcon.NOTES, apps.first().target)
        render {
            HomeScreen(HomeLayout.GRID, "14:03", "Fri, 30 Oct", {}, {}, {}, leftShortcut = left,
                rightShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.HEADPHONES, apps.last().target),
                onCorner = { chosen = it }, cornerLabel = { if (it == CornerPosition.LEFT) "Notes" else "Podcasts" }, shortcuts = legacyHomeShortcuts())
        }
        rule.onNodeWithContentDescription("Notes").performClick()
        assertEquals(CornerPosition.LEFT, chosen)
        rule.onNodeWithContentDescription("Podcasts").performClick()
        assertEquals(CornerPosition.RIGHT, chosen)
        capture("home-corner-shortcuts")
    }

    @Test fun `corner settings changes the icon and either action without altering fixed apps`() {
        var p by mutableStateOf(LauncherPreferences(leftShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.NOTES, apps.first().target)))
        var picker: CornerPosition? = null
        render {
            Column(Modifier.fillMaxSize()) { QuickCornerSettings(p, apps, { p = it(p) }, { picker = it }) }
        }
        rule.onNodeWithText("Icon").performClick()
        rule.onNodeWithTag("icon-search").performTextInput("camera")
        rule.onNodeWithContentDescription("Camera").performClick()
        rule.onNodeWithText("Use icon").performClick()
        assertEquals(ShortcutIcon.CAMERA, p.leftShortcut.icon)
        rule.onNodeWithText("Settings", substring = false).assertDoesNotExist()
        rule.onAllNodesWithText("App").onLast().performClick()
        assertEquals(CornerPosition.RIGHT, picker)
        assertTrue(p.homeShortcuts.isEmpty())
        capture("corner-settings")
    }

    @Test fun `unlock reveal leaves header fixed and does not replay on ordinary recomposition`() {
        var locked by mutableStateOf(false)
        var unlock by mutableStateOf<UnlockRevealRequest?>(null)
        var time by mutableStateOf("14:03")
        render { HomeScreen(HomeLayout.GRID, time, "Fri, 30 Oct", {}, {}, {}, unlockRequest = unlock, deviceLocked = locked, shortcuts = legacyHomeShortcuts()) }
        val bounds = rule.onNodeWithTag("home-clock").fetchSemanticsNode().boundsInRoot
        rule.mainClock.autoAdvance = false
        // Flush snapshot writes explicitly before advancing the paused test frame clock.
        rule.runOnIdle { locked = true; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(32)
        rule.onNodeWithContentDescription("Music").assertIsNotEnabled()
        rule.runOnIdle { locked = false; unlock = UnlockRevealRequest(); Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(64)
        assertEquals(bounds, rule.onNodeWithTag("home-clock").fetchSemanticsNode().boundsInRoot)
        rule.onNodeWithContentDescription("Music").assertIsNotEnabled()
        capture("unlock-reveal-middle")
        rule.mainClock.advanceTimeBy(700)
        rule.onNodeWithContentDescription("Music").assertIsEnabled()
        rule.runOnIdle { time = "14:04"; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("14 04").assertExists()
        rule.onNodeWithContentDescription("Music").assertIsEnabled()
    }

    @Test fun `reduced motion keeps drawer at its final position during its short fade`() {
        var open by mutableStateOf(false)
        render { drawer(LauncherPreferences(motionMode = MotionMode.REDUCED), open, { open = it }) }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("Apps").performClick()
        rule.mainClock.advanceTimeBy(48)
        assertTrue(progress() in 0f..0.99f)
        val top = rule.onNodeWithTag("apps-screen", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top
        assertEquals(0f, top, 1f)
        rule.mainClock.advanceTimeBy(250)
        assertTrue(open)
    }

    @Test fun `drawer animation frames render for visual review`() {
        var open by mutableStateOf(false)
        render { drawer(LauncherPreferences(), open, { open = it }) }
        rule.mainClock.autoAdvance = false
        capture("drawer-motion-00")
        rule.onNodeWithContentDescription("Apps").performClick()
        for (frame in 1..8) {
            rule.mainClock.advanceTimeBy(40)
            capture("drawer-motion-" + frame.toString().padStart(2, '0'))
        }
        assertTrue(open)
        rule.onNodeWithText("Back").performClick()
        for (frame in 9..16) {
            rule.mainClock.advanceTimeBy(40)
            capture("drawer-motion-" + frame.toString().padStart(2, '0'))
        }
        assertFalse(open)
    }
}
