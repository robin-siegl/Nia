package dev.robinsiegl.nia

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
class HomeCustomizationTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var host: android.view.View
    private val maps = AppEntry(AppTarget("example.maps", ".Main"), "Google Maps")
    private val notes = AppEntry(AppTarget("example.notes", ".Main"), "Notes")
    private val entries = (1..8).map { HomeShortcut("app-$it", "App $it", ShortcutIcon.entries[it], maps.target) }
    private fun render(light: Boolean = false, large: Boolean = false, content: @Composable () -> Unit) {
        val p = LauncherPreferences(themeMode = if (light) ThemeMode.LIGHT else ThemeMode.DARK)
        rule.setContent {
            host = LocalView.current
            NiaTheme(Palette.generate(p, false)) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, if (large) 2f else 1f)) {
                    Surface(Modifier.fillMaxSize(), content = content)
                }
            }
        }
    }
    private fun capture(name: String) = rule.runOnIdle {
        val bitmap = android.graphics.Bitmap.createBitmap(host.width, host.height, android.graphics.Bitmap.Config.ARGB_8888)
        host.draw(android.graphics.Canvas(bitmap))
        File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }.outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    @Test fun `fresh Home has only Apps and independent Clock and Calendar actions`() {
        var clocks = 0; var dates = 0; var apps = 0
        render { HomeScreen(HomeLayout.GRID, "14:03", "Fri, 30 Oct", { clocks++ }, {}, { apps++ }, onDate = { dates++ }) }
        rule.onNodeWithContentDescription("Phone").assertDoesNotExist()
        rule.onNodeWithTag("home-clock").performClick()
        rule.onNodeWithTag("home-date").performClick()
        rule.onNodeWithContentDescription("Apps").performClick()
        assertEquals(1, clocks); assertEquals(1, dates); assertEquals(1, apps)
        capture("home-apps-only")
    }
    @Test fun `nine grid entries fit three rows without scrolling and launch by stable identity`() {
        var selected = ""
        var scrollable = true
        render(large = true) { HomeScreen(HomeLayout.GRID, "14:03", "Fri, 30 Oct", {}, { selected = it }, {},
            shortcuts = entries, onScrollable = { scrollable = it }) }
        val bounds = entries.map { rule.onNodeWithContentDescription(it.name).assertIsDisplayed().fetchSemanticsNode().boundsInRoot }
        rule.onNodeWithContentDescription("Apps").assertIsDisplayed()
        assertEquals(3, bounds.map { it.top }.distinct().size)
        assertFalse(scrollable)
        rule.onNodeWithContentDescription("App 8").performClick()
        assertEquals("app-8", selected)
        capture("home-nine-grid")
    }
    @Test @Config(qualifiers = "w320dp-h480dp-xhdpi")
    fun `compact grid fallback keeps nine minimum touch targets with a large classic clock`() {
        render(large = true) { HomeScreen(HomeLayout.GRID, "14:03", "Fri, 30 Oct", {}, {}, {},
            pixelClockLayout = false, textSize = HomeTextSize.LARGE, shortcuts = entries) }
        rule.onNodeWithTag("compact-home-header").assertIsDisplayed()
        val minimum = 48 * host.resources.displayMetrics.density
        (entries.map { it.name } + "Apps").forEach {
            val bounds = rule.onNodeWithContentDescription(it).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.height >= minimum)
            assertTrue(bounds.bottom <= host.height - 64 * host.resources.displayMetrics.density)
        }
        capture("home-nine-grid-compact")
    }
    @Test @Config(qualifiers = "w320dp-h600dp-xhdpi")
    fun `large text list scrolls while Apps and corners remain reachable`() {
        var apps = 0
        var corner = 0
        render(light = true, large = true) { HomeScreen(HomeLayout.LIST, "14:03", "Fri, 30 Oct", {}, {}, { apps++ },
            textSize = HomeTextSize.LARGE, shortcuts = entries,
            rightShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.CAMERA, maps.target), onCorner = { corner++ }) }
        rule.onNodeWithTag("pinned-home-apps").assertIsDisplayed()
        rule.onNodeWithTag("home-entry-App 8").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("compact-home-header").assertIsDisplayed()
        capture("home-nine-list-large")
        rule.onNodeWithTag("pinned-home-apps").performClick()
        rule.onNodeWithTag("home-corner-RIGHT").performClick()
        assertEquals(1, apps); assertEquals(1, corner)
    }
    @Test fun `changing app keeps the independently chosen name and outline icon`() {
        var draft by mutableStateOf(HomeShortcutDraft(HomeShortcut("maps", "Maps", ShortcutIcon.MAPS, maps.target), false))
        var choosing by mutableStateOf(false)
        var saved: HomeShortcut? = null
        render {
            if (choosing) AppListScreen(listOf(notes), false, false, onBack = { choosing = false }, onSelect = {
                draft = draft.copy(shortcut = draft.shortcut.copy(app = it.target)); choosing = false
            }) else HomeShortcutEditorScreen(draft, listOf(maps, notes), false,
                onChange = { draft = draft.copy(shortcut = it(draft.shortcut)) }, onChooseApp = { choosing = true },
                onSave = { saved = draft.shortcut }, onRemove = {}, onBack = {})
        }
        rule.onNodeWithTag("home-shortcut-name").performTextReplacement("Google Maps")
        capture("home-shortcut-editor")
        rule.onNodeWithText("Opens").performClick()
        rule.onNodeWithText("Notes").performClick()
        assertEquals("Google Maps", draft.shortcut.name)
        assertEquals(ShortcutIcon.MAPS, draft.shortcut.icon)
        rule.onNodeWithText("Icon").performClick()
        rule.onNodeWithContentDescription("Star").performClick()
        rule.onNodeWithText("Save").performClick()
        assertEquals(draft.shortcut, saved)
        assertEquals(notes.target, saved!!.app)
    }
    @Test fun `new shortcuts require both a name and an app and can be canceled without changing preferences`() {
        var backs = 0; var saves = 0
        var draft by mutableStateOf(HomeShortcutDraft(HomeShortcut("new", ""), true))
        val original = LauncherPreferences()
        render { HomeShortcutEditorScreen(draft, listOf(maps), false,
            { draft = draft.copy(shortcut = it(draft.shortcut)) }, {}, { saves++ }, {}, { backs++ }) }
        rule.onNodeWithText("Save").assertIsNotEnabled()
        rule.onNodeWithTag("home-shortcut-name").performTextInput("Maps")
        rule.onNodeWithText("Save").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backs); assertEquals(0, saves)
        assertTrue(original.homeShortcuts.isEmpty())
    }
    @Test fun `arrangement editor reorders in one shared list and keeps Apps reserved at the limit`() {
        var p by mutableStateOf(LauncherPreferences(homeShortcuts = entries))
        render(light = true) { SettingsLayout("Home apps", {}) {
            HomeAppsSettings(p, listOf(maps), {}, { id, direction ->
                val next = p.homeShortcuts.toMutableList(); val index = next.indexOfFirst { it.id == id }
                next.add(index + direction, next.removeAt(index)); p = p.copy(homeShortcuts = next)
            })
        } }
        rule.onNodeWithContentDescription("Move App 1 up").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Move App 1 down").performClick()
        assertEquals("app-2", p.homeShortcuts.first().id)
        rule.onNodeWithText("Add app").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Always available · stays last").assertIsDisplayed()
        capture("home-arrangement")
    }
    @Test fun `removal is explicit and canceling preserves the shortcut`() {
        var removals = 0
        val draft = HomeShortcutDraft(entries.first(), false)
        render { HomeShortcutEditorScreen(draft, listOf(maps), false, {}, {}, {}, { removals++ }, {}) }
        rule.onNodeWithText("Remove from Home").performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(0, removals)
        rule.onNodeWithText("Remove from Home").performClick()
        rule.onNodeWithText("Remove", substring = false).performClick()
        assertEquals(1, removals)
    }
}
