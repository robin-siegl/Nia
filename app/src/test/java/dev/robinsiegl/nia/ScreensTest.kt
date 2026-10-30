package dev.robinsiegl.nia

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
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
class ScreensTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var host: android.view.View

    private fun render(preferences: LauncherPreferences = LauncherPreferences(themeMode = ThemeMode.DARK),
        content: @Composable () -> Unit) {
        rule.setContent {
            host = LocalView.current
            NiaTheme(Palette.generate(preferences, false)) {
                Surface(Modifier.fillMaxSize(), content = content)
            }
        }
    }

    private fun screenshot(name: String): android.graphics.Bitmap =
        rule.runOnIdle {
            val bitmap = android.graphics.Bitmap.createBitmap(host.width, host.height, android.graphics.Bitmap.Config.ARGB_8888)
            host.draw(android.graphics.Canvas(bitmap))
            val output = File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }
            output.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bitmap
        }

    @Test fun `grid provides all five accessible shortcuts and opens the chosen category`() {
        var selected: HomeCategory? = null
        render { HomeScreen(HomeLayout.GRID, "09:41", "Friday, October 30", {}, { selected = it }, {}) }
        HomeCategory.entries.forEach { rule.onNodeWithContentDescription(it.label).assertIsDisplayed() }
        rule.onNodeWithText("Music").assertDoesNotExist()
        rule.onNodeWithText("Apps").assertDoesNotExist()
        rule.onNodeWithContentDescription("Apps").assertIsDisplayed()
        screenshot("home-oled-grid")
        rule.onNodeWithContentDescription("Music").performClick()
        assertEquals(HomeCategory.MUSIC, selected)
    }

    @Test fun `text home provides the same categories and Apps button`() {
        var opened = false
        render(LauncherPreferences(themeMode = ThemeMode.LIGHT)) {
            HomeScreen(HomeLayout.LIST, "09:41", "Friday, October 30", {}, {}, { opened = true })
        }
        HomeCategory.entries.forEach { rule.onNodeWithText(it.label).assertIsDisplayed() }
        assertEquals(rule.onNodeWithTag("home-entry-Music").fetchSemanticsNode().boundsInRoot.height,
            rule.onNodeWithTag("home-entry-Apps").fetchSemanticsNode().boundsInRoot.height, 0f)
        screenshot("home-light-list")
        rule.onNodeWithText("Apps").performClick()
        assertTrue(opened)
    }

    @Test fun `Apps icon and Home settings gear open their destinations`() {
        var openedApps = false
        var openedSettings = false
        render {
            HomeScreen(HomeLayout.GRID, "09:41", "Friday, October 30", {}, {}, { openedApps = true },
                onSettings = { openedSettings = true })
        }
        rule.onNodeWithContentDescription("Apps").performClick()
        rule.onNodeWithContentDescription("Settings").performClick()
        assertTrue(openedApps && openedSettings)
    }

    @Test @Config(qualifiers = "w320dp-h600dp-xhdpi")
    fun `large Home text and system font scaling keep Apps and settings reachable`() {
        var opened = false
        render {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                HomeScreen(HomeLayout.LIST, "09:41", "Friday, October 30", {}, {}, { opened = true },
                    textSize = HomeTextSize.LARGE)
            }
        }
        rule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        rule.onNodeWithText("Apps").performScrollTo().performClick()
        assertTrue(opened)
        screenshot("home-large-system-text")
    }

    @Test fun `swipe up opens Apps`() {
        var opened = false
        render { HomeScreen(HomeLayout.GRID, "09:41", "Friday, October 30", {}, {}, { opened = true }) }
        rule.onRoot().performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertTrue(opened)
    }

    @Test fun `app list searches by name and Clear restores all apps`() {
        val apps = listOf("Calendar", "École", "Music").map { AppEntry(AppTarget("test.app", it), it) }
        render { AppListScreen(apps, false, false, onBack = {}, onSelect = {}) }
        screenshot("apps-oled")
        rule.onNodeWithText("Search apps").performTextInput("eco")
        rule.onNodeWithText("École").assertIsDisplayed()
        rule.onNodeWithText("Music").assertDoesNotExist()
        rule.onNodeWithText("Clear").performClick()
        rule.onNodeWithText("Music").assertIsDisplayed()
    }

    @Test fun `autofocus can be enabled`() {
        render { AppListScreen(emptyList(), false, true, onBack = {}, onSelect = {}) }
        rule.onNodeWithText("Search apps").assertIsFocused()
    }

    @Test fun `autofocus can be disabled`() {
        render { AppListScreen(emptyList(), false, false, onBack = {}, onSelect = {}) }
        rule.onNodeWithText("Search apps").assertIsNotFocused()
    }

    @Test fun `app list replaces settings text with a gear`() {
        var opened = false
        render { AppListScreen(emptyList(), false, false, onBack = {}, onSelect = {}, onSettings = { opened = true }) }
        rule.onNodeWithText("nia settings").assertDoesNotExist()
        rule.onNodeWithContentDescription("Settings").performClick()
        assertTrue(opened)
    }

    @Test fun `alphabet edge gesture reaches the final letter and search hides the scrubber`() {
        val apps = ('A'..'Z').flatMap { letter -> (0..2).map { AppEntry(AppTarget("test.app", "$letter$it"), "$letter app $it") } }
        var launched = false
        render {
            AppListScreen(apps, false, false, onBack = {}, onSelect = { launched = true }, onSettings = {}, alphabetScrubber = true)
        }
        rule.onNodeWithTag("alphabet-scrubber").performTouchInput { down(topCenter); moveTo(bottomCenter, 400) }
        rule.onNodeWithText("Z").assertIsDisplayed()
        screenshot("apps-alphabet-active")
        rule.onNodeWithTag("alphabet-scrubber").performTouchInput { up() }
        rule.onNodeWithText("Z").assertDoesNotExist()
        rule.onNodeWithText("Z app 2").assertIsDisplayed()
        assertFalse(launched)
        screenshot("apps-alphabet")
        rule.onNodeWithText("Search apps").performTextInput("M app")
        rule.onNodeWithTag("alphabet-scrubber").assertDoesNotExist()
        rule.onNodeWithText("M app 0").assertIsDisplayed()
        rule.onNodeWithText("Clear").performClick()
        rule.onNodeWithTag("alphabet-scrubber").assertIsDisplayed()
    }

    @Test fun `alphabet navigation supports accessibility progress actions`() {
        val apps = ('A'..'Z').map { AppEntry(AppTarget("test.app", "$it"), "$it app") }
        render { AppListScreen(apps, false, false, onBack = {}, onSelect = {}, alphabetScrubber = true) }
        rule.onNodeWithTag("alphabet-scrubber").performSemanticsAction(SemanticsActions.SetProgress) { it(25f) }
        rule.onNodeWithText("Z app").assertIsDisplayed()
    }

    @Test fun `alphabet navigation is optional and absent for empty results`() {
        val apps = listOf("Calendar", "Music").map { AppEntry(AppTarget("test.app", it), it) }
        render { AppListScreen(apps, false, false, onBack = {}, onSelect = {}) }
        rule.onNodeWithTag("alphabet-scrubber").assertDoesNotExist()
    }

    @Test fun `fractional accessibility adjustments move between two alphabet sections`() {
        var first by mutableIntStateOf(0)
        render { AlphabetScrubber(listOf(AlphabetSection("A", 0), AlphabetSection("Z", 10)), first, { first = it }) }
        val scrubber = rule.onNodeWithTag("alphabet-scrubber")
        scrubber.performSemanticsAction(SemanticsActions.SetProgress) { it(0.1f) }
        scrubber.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Z"))
        scrubber.performSemanticsAction(SemanticsActions.SetProgress) { it(0.9f) }
        scrubber.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "A"))
    }

    @Test fun `violet light palette home renders`() {
        render(LauncherPreferences(colorStyle = ColorStyle.COLOR, themeMode = ThemeMode.LIGHT)) {
            HomeScreen(HomeLayout.LIST, "09:41", "Friday, October 30", {}, {}, {})
        }
        screenshot("home-violet-light")
    }

    @Test fun `violet dark palette draws the generated background`() {
        val p = LauncherPreferences(colorStyle = ColorStyle.COLOR, themeMode = ThemeMode.DARK)
        render(p) { HomeScreen(HomeLayout.LIST, "09:41", "Friday, October 30", {}, {}, {}) }
        val bitmap = screenshot("home-violet-dark")
        assertEquals(Palette.generate(p, false).background, bitmap.getPixel(0, 0))
    }

    @Test fun `color settings expose system mode and paired previews`() {
        val p = LauncherPreferences(colorStyle = ColorStyle.COLOR, themeMode = ThemeMode.DARK)
        render(p) {
            SettingsScreen(p, emptyList(), false, false, {}, {}, {}, {}, {}, {}, {}, {}, {})
        }
        rule.onNodeWithText("System").assertIsDisplayed()
        screenshot("settings-violet-dark")
    }

    @Test fun `Home text size selection updates its preview and preference`() {
        var p by mutableStateOf(LauncherPreferences(layout = HomeLayout.LIST))
        render {
            SettingsScreen(p, emptyList(), false, false, {}, { change -> p = change(p) }, {}, {}, {}, {}, {}, {}, {})
        }
        val before = rule.onNodeWithText("09:41").fetchSemanticsNode().boundsInRoot.height
        rule.onNodeWithText("Large").performClick()
        assertEquals(HomeTextSize.LARGE, p.homeTextSize)
        assertTrue(rule.onNodeWithText("09:41").fetchSemanticsNode().boundsInRoot.height > before)
        screenshot("settings-home-size")
    }

    @Test fun `color wheel and brightness save the chosen seed and render both previews`() {
        val p = LauncherPreferences(colorStyle = ColorStyle.COLOR, themeMode = ThemeMode.LIGHT)
        var changed = p
        var saves = 0
        render(p) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                ColorSettings(p) { changed = it(changed); saves++ }
            }
        }
        rule.onNodeWithTag("color-preview-Light").assertIsDisplayed()
        rule.onNodeWithTag("color-preview-Dark").assertIsDisplayed()
        screenshot("color-picker")
        rule.onNodeWithTag("color-wheel").performTouchInput { down(centerRight) }
        rule.onNodeWithText("#ED0000").assertIsDisplayed()
        assertEquals(0, saves)
        rule.onNodeWithTag("color-wheel").performTouchInput { up() }
        assertEquals("#ED0000", changed.seedColor)
        assertEquals(1, saves)
        rule.onNodeWithContentDescription("Color brightness").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        assertEquals("#800000", changed.seedColor)
    }

    @Test fun `hex color previews accept valid input and prevent invalid saves`() {
        val p = LauncherPreferences(colorStyle = ColorStyle.COLOR)
        var changed = p
        render(p) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                ColorSettings(p) { changed = it(changed) }
            }
        }
        rule.onNodeWithText("Hex color").performTextReplacement("invalid")
        rule.onNodeWithText("Apply").assertIsNotEnabled()
        assertEquals(p, changed)
        rule.onNodeWithText("Hex color").performTextReplacement("#00aaff")
        rule.onNodeWithText("Apply").performClick()
        assertEquals("#00AAFF", changed.seedColor)
    }
}
