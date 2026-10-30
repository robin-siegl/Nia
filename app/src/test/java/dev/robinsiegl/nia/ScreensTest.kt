package dev.robinsiegl.nia

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        screenshot("home-light-list")
        rule.onNodeWithText("Apps").performClick()
        assertTrue(opened)
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
}
