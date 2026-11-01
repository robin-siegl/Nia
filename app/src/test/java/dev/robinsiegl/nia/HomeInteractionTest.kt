package dev.robinsiegl.nia

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
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
class HomeInteractionTest {
    private val scale = object : MotionDurationScale { override var scaleFactor by mutableFloatStateOf(1f) }
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>(effectContext = scale)
    private lateinit var host: android.view.View
    private val entries = (1..9).map { HomeShortcut("app-$it", "App $it", ShortcutIcon.entries[it], AppTarget("example.app", ".App$it")) }
    private var p by mutableStateOf(LauncherPreferences(themeMode = ThemeMode.DARK))
    private var open by mutableStateOf(false)
    private var search by mutableStateOf(false)
    private var launches = 0
    private var settings = 0
    private var commits = 0
    private val cues = mutableListOf<PullCue>()

    @Suppress("DEPRECATION")
    private fun render(layout: HomeLayout = HomeLayout.GRID, shortcuts: List<HomeShortcut> = entries,
        large: Boolean = false, light: Boolean = false, content: (@Composable () -> Unit)? = null) {
        if (large) rule.activity.resources.let { resources ->
            resources.updateConfiguration(Configuration(resources.configuration).apply { fontScale = 2f }, resources.displayMetrics)
        }
        p = p.copy(layout = layout, homeTextSize = if (large) HomeTextSize.LARGE else HomeTextSize.DEFAULT,
            themeMode = if (light) ThemeMode.LIGHT else ThemeMode.DARK, homeShortcuts = shortcuts)
        rule.setContent {
            host = LocalView.current
            NiaTheme(Palette.generate(p, false)) {
                Surface(Modifier.fillMaxSize()) {
                    if (content != null) content() else HomeAppsScreen(p, emptyList(), false, "14:03", "Fri, 30 Oct", open,
                        { open = it }, {}, { id -> when (p.homeShortcuts.first { it.id == id }.command) {
                            ShortcutCommand.OPEN_APPS -> { search = false; open = true }
                            ShortcutCommand.SEARCH_APPS -> { search = true; open = true }
                            else -> launches++
                        } }, { settings++ }, {}, {}, {}, searchAppsOnOpen = search,
                        onPullCue = { cues += it }, onReorderHome = { ids ->
                            commits++; p = p.reorderHome(ids); true
                        })
                }
            }
        }
        rule.mainClock.autoAdvance = false
    }
    private fun collapse() = rule.onNode(SemanticsMatcher.keyIsDefined(HomeHeaderCollapse), useUnmergedTree = true).fetchSemanticsNode().config[HomeHeaderCollapse]
    private fun progress() = rule.onNodeWithTag("home-apps-surface").fetchSemanticsNode().config[DrawerProgress]
    private fun root() = rule.onNodeWithTag("home-apps-surface")
    private fun bounds(id: String) = rule.onNodeWithTag("home-entry-$id").fetchSemanticsNode().boundsInRoot
    private fun capture(name: String) = rule.runOnIdle {
        val view = android.view.inspector.WindowInspector.getGlobalWindowViews().lastOrNull { it.isShown && it.width > 0 && it.height > 0 } ?: host
        val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }.outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun `grid clock morphs continuously and carries the icons upward without duplicate headers`() {
        render()
        val first = bounds("app-1")
        val clock = rule.onNodeWithTag("home-clock").fetchSemanticsNode().boundsInRoot
        capture("home-scroll-expanded")
        root().performTouchInput { down(Offset(width * 0.5f, height * 0.75f)); moveBy(Offset(0f, -120f), 300) }
        rule.mainClock.advanceTimeBy(32)
        assertTrue(collapse() in 0.05f..0.95f)
        assertTrue(bounds("app-1").top < first.top)
        assertTrue(rule.onNodeWithTag("home-clock").fetchSemanticsNode().boundsInRoot.width < clock.width)
        rule.onAllNodesWithTag("home-clock").assertCountEquals(1)
        rule.onAllNodesWithTag("home-date").assertCountEquals(1)
        assertTrue(cues.isEmpty())
        capture("home-scroll-mid")
        root().performTouchInput { moveBy(Offset(0f, -200f), 300) }
        rule.mainClock.advanceTimeBy(32)
        assertEquals(1f, collapse(), 0.01f)
        capture("home-scroll-compact")
        rule.onNodeWithText("Pull to open Apps").assertDoesNotExist()
        rule.onNodeWithText("Release to open Apps").assertDoesNotExist()
        root().performTouchInput { cancel() }
        rule.mainClock.advanceTimeBy(500)
        assertEquals(0f, progress(), 0f)
    }
    @Test @Config(qualifiers = "w320dp-h600dp-xhdpi")
    fun `eight apps plus an optional Apps command scroll together at maximum text then open the drawer`() {
        render(HomeLayout.LIST, entries.take(8) + appsCommand(), large = true, light = true)
        rule.onNodeWithTag("home-entry-apps-command").assert(hasAnyAncestor(hasScrollAction()))
        rule.onNodeWithTag("home-entry-apps-command").assertIsNotDisplayed()
        rule.onNodeWithTag("pinned-home-apps").assertDoesNotExist()
        var attempts = 0
        while (!open && attempts++ < 6) {
            rule.onNodeWithTag("home-list").performTouchInput { swipeUp(startY = height * 0.9f, endY = height * 0.1f, durationMillis = 600) }
            rule.mainClock.advanceTimeBy(600)
        }
        assertTrue("List scrolling never handed off to Apps", open)
        assertEquals(1f, progress(), 0f)
        assertEquals(listOf(PullCue.ARMED), cues)
        rule.onNodeWithText("Back").performClick()
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithTag("home-entry-apps-command").assertIsDisplayed()
        capture("home-nine-list-large")
    }
    @Test fun `an empty grid still opens Apps by gesture without a generated entry`() {
        render(shortcuts = emptyList())
        rule.onNodeWithContentDescription("Apps").assertDoesNotExist()
        root().performTouchInput { swipeUp() }
        rule.mainClock.advanceTimeBy(500)
        assertTrue(open)
    }
    @Test fun `grid drag crosses rows and saves once on drop without launching or opening Settings`() {
        render()
        val original = p.homeShortcuts
        val start = bounds("app-1").center
        val destination = bounds("app-8").center
        root().performTouchInput { down(start); advanceEventTime(600) }
        rule.mainClock.advanceTimeBy(650)
        root().performTouchInput { moveTo(destination, 500) }
        rule.mainClock.advanceTimeBy(240)
        assertEquals(0, commits)
        assertEquals(original, p.homeShortcuts)
        capture("home-drag-preview")
        assertTrue("Lifted shortcut did not follow the finger: ${bounds("app-1")} / $destination",
            (bounds("app-1").center - destination).getDistance() < 20f)
        root().performTouchInput { up() }
        rule.mainClock.advanceTimeBy(500)
        assertEquals(1, commits)
        assertEquals("app-1", p.homeShortcuts[7].id)
        assertEquals(original.map { it.id }.toSet(), p.homeShortcuts.map { it.id }.toSet())
        assertEquals(0, launches); assertEquals(0, settings); assertEquals(0f, progress(), 0f)
    }
    @Test fun `canceling a Home drag writes nothing and restores normal taps and Apps gestures`() {
        render()
        val original = p.homeShortcuts
        val start = bounds("app-2").center
        val destination = bounds("app-6").center
        root().performTouchInput { down(start); advanceEventTime(600) }
        rule.mainClock.advanceTimeBy(650)
        root().performTouchInput { moveTo(destination, 400) }
        rule.mainClock.advanceTimeBy(200)
        root().performTouchInput { cancel() }
        rule.mainClock.advanceTimeBy(500)
        assertEquals(original, p.homeShortcuts); assertEquals(0, commits); assertEquals(0, settings)
        rule.onNodeWithTag("home-entry-app-2").performTouchInput { click() }
        assertEquals(1, launches)
        root().performTouchInput { swipeUp() }
        rule.mainClock.advanceTimeBy(500)
        assertTrue(open)
    }
    @Test fun `Settings uses the same drag preview and saves a single shared order`() {
        render(shortcuts = entries.take(4), content = {
            val scroll = rememberScrollState()
            SettingsLayout("Home shortcuts", {}, scroll) {
                HomeAppsSettings(p, emptyList(), {}, { _, _ -> }, { ids -> commits++; p = p.reorderHome(ids); true }, scroll)
            }
        })
        val first = rule.onNodeWithTag("home-editor-row-app-1").fetchSemanticsNode().boundsInRoot.center
        val third = rule.onNodeWithTag("home-editor-row-app-3").fetchSemanticsNode().boundsInRoot.center
        val surface = rule.onNodeWithTag("settings-content")
        val origin = surface.fetchSemanticsNode().boundsInRoot.topLeft
        surface.performTouchInput { down(first - origin); advanceEventTime(600) }
        rule.mainClock.advanceTimeBy(650)
        surface.performTouchInput { moveTo(third - origin, 400) }
        rule.mainClock.advanceTimeBy(240)
        surface.performTouchInput { up() }
        rule.mainClock.advanceTimeBy(500)
        assertEquals(1, commits)
        assertEquals(listOf("app-2", "app-3", "app-1", "app-4"), p.homeShortcuts.map { it.id })
        capture("home-arrangement")
    }
    @Test fun `Search command focuses only after settling and leaves the saved preference off`() {
        render(shortcuts = listOf(HomeShortcut("search", "Find an app", ShortcutIcon.SEARCH, command = ShortcutCommand.SEARCH_APPS)))
        rule.onNodeWithContentDescription("Find an app").performTouchInput { click() }
        rule.mainClock.advanceTimeBy(32)
        rule.onNodeWithTag("apps-search", useUnmergedTree = true).assertIsNotFocused()
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("Search apps").assertIsFocused()
        assertFalse(p.searchAutofocus)
    }
    @Test fun `disabled animations still allow rearrangement and the next Apps gesture`() {
        scale.scaleFactor = 0f
        render(shortcuts = entries.take(3))
        val start = bounds("app-1").center
        val destination = bounds("app-3").center
        root().performTouchInput { down(start); advanceEventTime(600) }
        rule.mainClock.advanceTimeBy(650)
        root().performTouchInput { moveTo(destination, 400); up() }
        rule.mainClock.advanceTimeBy(64)
        assertEquals("app-1", p.homeShortcuts.last().id)
        root().performTouchInput { swipeUp() }
        rule.mainClock.advanceTimeBy(64)
        assertTrue(open)
    }
    @Test @Config(qualifiers = "w320dp-h600dp-xhdpi")
    fun `a held list drag auto-scrolls to off-screen entries without arming Apps`() {
        render(HomeLayout.LIST, large = true)
        val original = p.homeShortcuts
        val start = bounds("app-1").center
        val viewport = rule.onNodeWithTag("home-list").fetchSemanticsNode().boundsInRoot
        val edge = Offset(viewport.center.x, viewport.bottom - 48f)
        root().performTouchInput { down(start); advanceEventTime(600) }
        rule.mainClock.advanceTimeBy(650)
        root().performTouchInput { moveTo(edge, 400) }
        rule.mainClock.advanceTimeBy(2_400)
        assertEquals(original, p.homeShortcuts); assertEquals(0, commits)
        assertEquals(0f, progress(), 0f); assertTrue(cues.isEmpty())
        capture("home-list-drag")
        root().performTouchInput { up() }
        rule.mainClock.advanceTimeBy(500)
        assertEquals(1, commits)
        assertEquals("app-1", p.homeShortcuts.last().id)
        assertEquals(original.map { it.id }.toSet(), p.homeShortcuts.map { it.id }.toSet())
        assertEquals(0, launches); assertEquals(0, settings)
    }
    @Test fun `Home exposes an accessible reordering alternative with the same saved order`() {
        render(shortcuts = entries.take(3))
        val action = rule.onNodeWithTag("home-entry-app-1").fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .first { it.label == "Move down" }
        rule.runOnIdle { assertTrue(action.action()) }
        rule.mainClock.advanceTimeBy(500)
        assertEquals(1, commits)
        assertEquals(listOf("app-2", "app-1", "app-3"), p.homeShortcuts.map { it.id })
    }
    @Test fun `a quick swipe from empty space below a fitting list opens Apps`() {
        render(HomeLayout.LIST, shortcuts = entries.take(2))
        root().performTouchInput { swipeUp() }
        rule.mainClock.advanceTimeBy(500)
        assertTrue(open)
    }
    @Test @Config(qualifiers = "w320dp-h600dp-xhdpi")
    fun `a quick swipe at the end of a large text list opens Apps too`() {
        render(HomeLayout.LIST, large = true)
        rule.mainClock.autoAdvance = true
        rule.onNodeWithTag("home-list").performTouchInput {
            swipeUp(startY = height * 0.9f, endY = height * 0.6f, durationMillis = 300)
        }
        rule.onNodeWithTag("home-entry-app-9").performScrollTo()
        rule.mainClock.autoAdvance = false
        root().performTouchInput { swipeUp() }
        rule.mainClock.advanceTimeBy(500)
        assertTrue(open)
    }
}
