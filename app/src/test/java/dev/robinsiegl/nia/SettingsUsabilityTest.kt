package dev.robinsiegl.nia

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h892dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsUsabilityTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var host: android.view.View

    private fun render(light: Boolean = false, largeText: Boolean = false, content: @Composable () -> Unit) {
        val p = LauncherPreferences(themeMode = if (light) ThemeMode.LIGHT else ThemeMode.DARK)
        rule.setContent {
            host = LocalView.current
            NiaTheme(Palette.generate(p, false)) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, if (largeText) 2f else 1f)) {
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

    @Composable
    private fun Detail(section: SettingsSection, storageError: Boolean = false,
        onBack: () -> Unit = {}, onResetAppearance: () -> Unit = {}, onResetAll: () -> Unit = {}) {
        SettingsDetailScreen(section, LauncherPreferences(), emptyList(), storageError, onBack, {}, {}, {}, {}, {},
            onResetAppearance, onResetAll, {})
    }

    @Test fun `switch label and control share one accessible action and toggle once per tap`() {
        var checked by mutableStateOf(false)
        var changes = 0
        render { SettingSwitch("Keyboard on open", "Open the keyboard when Apps is ready to search.", checked) {
            checked = it; changes++
        } }
        val switches = rule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
        switches.assertCountEquals(1)
        switches.onFirst().assertIsOff()
        rule.onNodeWithText("Keyboard on open", useUnmergedTree = true).performTouchInput { click() }
        assertTrue(checked); assertEquals(1, changes)
        switches.onFirst().assertIsOn()
        switches.onFirst().performTouchInput { click(centerRight - androidx.compose.ui.geometry.Offset(24f, 0f)) }
        assertFalse(checked); assertEquals(2, changes)
    }

    @Test fun `choice controls expose exactly one selected option and update their selection`() {
        var layout by mutableStateOf(HomeLayout.GRID)
        render { ChoiceRow(listOf(HomeLayout.GRID to "Icon grid", HomeLayout.LIST to "Text list"), layout, { layout = it }) }
        rule.onNodeWithText("Icon grid").assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        rule.onNodeWithText("Text list").assertIsNotSelected().performClick()
        assertEquals(HomeLayout.LIST, layout)
        rule.onNodeWithText("Text list").assertIsSelected()
        rule.onNodeWithText("Icon grid").assertIsNotSelected()
    }

    @Test @Config(qualifiers = "w320dp-h600dp-xhdpi")
    fun `Back stays visible after scrolling a narrow settings page at double font size`() {
        var backs = 0
        render(light = true, largeText = true) { Detail(SettingsSection.BACKUP, onBack = { backs++ }) }
        val before = rule.onNodeWithContentDescription("Back").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithText("Reset all settings").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Back").assertIsDisplayed()
        assertEquals(before, rule.onNodeWithContentDescription("Back").fetchSemanticsNode().boundsInRoot)
        capture("settings-backup-large-text")
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backs)
    }

    @Test fun `both resets require confirmation and cancellation leaves settings alone`() {
        var appearance = 0
        var all = 0
        render { Detail(SettingsSection.BACKUP, onResetAppearance = { appearance++ }, onResetAll = { all++ }) }
        rule.onNodeWithText("Reset appearance & behavior").performScrollTo().performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(0, appearance)
        rule.onNodeWithText("Reset all settings").performScrollTo().performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(0, all)
        rule.onNodeWithText("Reset appearance & behavior").performScrollTo().performClick()
        rule.onNode(hasText("Reset") and hasClickAction()).performClick()
        assertEquals(1, appearance); assertEquals(0, all)
    }

    @Test fun `recovery disables saving and appearance reset while keeping restore and full reset reachable`() {
        render { Detail(SettingsSection.BACKUP, storageError = true) }
        rule.onNodeWithText("Save backup").assertIsNotEnabled()
        rule.onNodeWithText("Restore backup").assertIsEnabled()
        rule.onNodeWithText("Reset appearance & behavior").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Reset all settings").performScrollTo().assertIsEnabled()
    }

    @Test fun `overview reflects saved choices and the non-default launcher action remains available`() {
        val p = LauncherPreferences(layout = HomeLayout.LIST, pixelClockLayout = false, themeMode = ThemeMode.DARK,
            searchAutofocus = true, assignments = mapOf(HomeCategory.MUSIC to AppTarget("example.music", ".Main")))
        var setDefault = 0
        var changeDefault = 0
        render(light = true) { SettingsScreen(false, false, {}, {}, { setDefault++ }, { changeDefault++ }, p) }
        rule.onNodeWithText("Text list · Classic clock").assertIsDisplayed()
        rule.onNodeWithText("Black & white · Dark").assertIsDisplayed()
        rule.onNodeWithText("1 of 5 shortcuts assigned").assertIsDisplayed()
        rule.onNodeWithText("Keyboard opens with Apps").performScrollTo().assertIsDisplayed()
        capture("settings-overview-light")
        rule.onNodeWithText("Default launcher").performScrollTo().performClick()
        assertEquals(1, changeDefault)
        rule.onNodeWithText("Set nia as default").performScrollTo().performClick()
        assertEquals(1, setDefault)
    }
}
