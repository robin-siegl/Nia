package dev.robinsiegl.nia

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import android.content.res.Configuration
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
class ShortcutIconPickerTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var host: android.view.View
    @Suppress("DEPRECATION")
    private fun render(light: Boolean = false, large: Boolean = false, content: @Composable () -> Unit) {
        val p = LauncherPreferences(themeMode = if (light) ThemeMode.LIGHT else ThemeMode.DARK)
        if (large) {
            val resources = rule.activity.resources
            resources.updateConfiguration(Configuration(resources.configuration).apply { fontScale = 2f }, resources.displayMetrics)
        }
        rule.setContent {
            host = LocalView.current
            NiaTheme(Palette.generate(p, false)) {
                Surface(Modifier.fillMaxSize(), content = content)
            }
        }
    }
    private fun capture(name: String) = rule.runOnIdle {
        val root = android.view.inspector.WindowInspector.getGlobalWindowViews().lastOrNull {
            it.isShown && it.width > 0 && it.height > 0
        } ?: host
        val bitmap = android.graphics.Bitmap.createBitmap(root.width, root.height, android.graphics.Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bitmap))
        File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }.outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    @Test fun `labeled browse and pending selection do not apply until confirmation`() {
        var chosen: ShortcutIcon? = null
        var showing by mutableStateOf(true)
        render { if (showing) ShortcutIconPicker("Choose icon", ShortcutIcon.CAMERA, { showing = false }, { chosen = it; showing = false }) }
        rule.onNodeWithText("192 icons").assertIsDisplayed()
        rule.onNodeWithTag("selected-icon-preview").assertIsDisplayed()
        capture("icon-picker-dark")
        rule.onNodeWithTag("icon-search").performTextInput("controller")
        rule.onNodeWithContentDescription("Gaming").performClick().assertIsSelected()
        rule.onNodeWithTag("icon-search").assertIsNotFocused()
        assertNull(chosen)
        capture("icon-picker-selection")
        rule.onNodeWithText("Use icon").performClick()
        assertEquals(ShortcutIcon.GAMING, chosen)
        rule.onNodeWithTag("shortcut-icon-picker").assertDoesNotExist()
    }
    @Test fun `opening the picker clears the underlying editor without changing its text`() {
        var showing by mutableStateOf(false)
        var name by mutableStateOf("")
        render {
            Column {
                OutlinedTextField(name, { name = it }, modifier = Modifier.testTag("parent-name"))
                Button(onClick = { showing = true }) { Text("Open picker") }
            }
            if (showing) ShortcutIconPicker("Choose icon", ShortcutIcon.STAR, { showing = false }, { showing = false })
        }
        rule.onNodeWithTag("parent-name").performTextInput("Maps")
        rule.onNodeWithTag("parent-name").assertIsFocused()
        rule.onNodeWithText("Open picker").performClick()
        rule.onNodeWithTag("parent-name").assertIsNotFocused()
        rule.onNodeWithTag("icon-search").assertIsNotFocused()
        rule.onNodeWithContentDescription("Close icon picker").performClick()
        rule.onNodeWithTag("parent-name").assertIsNotFocused()
        assertEquals("Maps", name)
    }
    @Test fun `keyboard selection retains tile focus until the user confirms`() {
        var chosen: ShortcutIcon? = null
        render { ShortcutIconPicker("Choose icon", ShortcutIcon.CAMERA, {}, { chosen = it }) }
        rule.onNodeWithTag("icon-search").performTextInput("controller")
        val tile = rule.onNodeWithContentDescription("Gaming")
        val dialogView = (tile.fetchSemanticsNode().root as ViewRootForTest).view
        // Enable keyboard navigation in the dialog's window, rather than the editor behind it.
        rule.runOnIdle { assertTrue(dialogView.requestFocusFromTouch()) }
        tile.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        tile.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        tile.assertIsFocused().assertIsSelected()
        assertNull(chosen)
        rule.onNodeWithText("Use icon").performClick()
        assertEquals(ShortcutIcon.GAMING, chosen)
    }
    @Test fun `cancel discards a preview and reopening starts from the saved choice`() {
        var chosen: ShortcutIcon? = null
        var showing by mutableStateOf(true)
        render { if (showing) ShortcutIconPicker("Choose icon", ShortcutIcon.CAMERA, { showing = false }, { chosen = it }) }
        rule.onNodeWithTag("icon-search").performTextInput("wifi")
        rule.onNodeWithContentDescription("Wi-Fi").performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertNull(chosen)
        rule.runOnIdle { showing = true }
        rule.onNodeWithTag("icon-search").performTextInput("camera")
        rule.onNodeWithContentDescription("Camera").assertIsSelected()
        rule.onNodeWithContentDescription("Close icon picker").performClick()
        assertNull(chosen)
    }
    @Test fun `category browsing has a clear selection and typing searches the entire collection`() {
        var chosen: ShortcutIcon? = null
        render(light = true) { ShortcutIconPicker("Choose icon", ShortcutIcon.STAR, {}, { chosen = it }) }
        rule.onNodeWithTag("icon-categories").performScrollToNode(hasText("Travel"))
        rule.onNodeWithText("Travel").performClick().assertIsSelected()
        rule.onNodeWithText("23 icons").assertIsDisplayed()
        capture("icon-picker-travel-light")
        rule.onNodeWithTag("icon-search").performTextInput("keyboard")
        rule.onNodeWithContentDescription("Keyboard").assertIsDisplayed().performClick()
        rule.onNodeWithText("Use icon").performClick()
        assertEquals(ShortcutIcon.KEYBOARD, chosen)
    }
    @Test fun `an empty search offers a way back to browsing`() {
        render { ShortcutIconPicker("Choose icon", ShortcutIcon.STAR, {}, {}) }
        rule.onNodeWithTag("icon-search").performTextInput("no-matching-symbol-xyz")
        rule.onNodeWithText("No icons found").assertIsDisplayed()
        rule.onNodeWithText("Clear search").performClick()
        rule.onNodeWithTag("shortcut-icon-grid").assertIsDisplayed()
        rule.onNodeWithText("192 icons").assertIsDisplayed()
    }
    @Test fun `pending icon and search survive activity state restoration without applying`() {
        var chosen: ShortcutIcon? = null
        val restore = StateRestorationTester(rule)
        restore.setContent { NiaTheme(Palette.generate(LauncherPreferences(themeMode = ThemeMode.DARK), true)) {
            ShortcutIconPicker("Choose icon", ShortcutIcon.CLOCK, {}, { chosen = it })
        } }
        rule.onNodeWithTag("icon-search").performTextInput("code")
        rule.onNodeWithContentDescription("Code").performClick()
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("icon-search").assertTextContains("code")
        rule.onNodeWithContentDescription("Code").assertIsSelected()
        assertNull(chosen)
        rule.onNodeWithText("Use icon").performClick()
        assertEquals(ShortcutIcon.CODE, chosen)
    }
    @Test @Config(qualifiers = "w320dp-h600dp-xhdpi")
    fun `compact picker keeps search selection and confirmation reachable at double text size`() {
        var chosen: ShortcutIcon? = null
        render(light = true, large = true) { ShortcutIconPicker("Home app icon", ShortcutIcon.STAR, {}, { chosen = it }) }
        rule.onNodeWithContentDescription("Close icon picker").assertIsDisplayed()
        rule.onNodeWithText("All icons").assertIsDisplayed().performClick()
        rule.onNodeWithText("Tech").performScrollTo().performClick()
        rule.onNodeWithText("Tech").assertIsDisplayed()
        rule.onNodeWithTag("icon-search").assertIsDisplayed().performTextInput("barcode")
        rule.onNodeWithContentDescription("QR code").assertIsDisplayed().performClick().assertIsSelected()
        rule.onNodeWithText("Use icon").assertIsDisplayed()
        capture("icon-picker-large-text")
        rule.onNodeWithText("Use icon").performClick()
        assertEquals(ShortcutIcon.QR_CODE, chosen)
    }
    @Test fun `Quick corners use the shared picker and only save a confirmed icon`() {
        val target = AppTarget("example.game", ".Main")
        val entries = legacyHomeShortcuts()
        var p by mutableStateOf(LauncherPreferences(homeShortcuts = entries,
            leftShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.PHONE, target)))
        var writes = 0
        render { Column(Modifier.fillMaxSize()) { QuickCornerSettings(p, listOf(AppEntry(target, "Game")), {
            p = it(p); writes++
        }, {}) } }
        rule.onNodeWithText("Icon").performClick()
        rule.onNodeWithTag("icon-search").performTextInput("controller")
        rule.onNodeWithContentDescription("Gaming").performClick()
        assertEquals(0, writes)
        rule.onNodeWithText("Use icon").performClick()
        assertEquals(1, writes)
        assertEquals(ShortcutIcon.GAMING, p.leftShortcut.icon)
        assertEquals(target, p.leftShortcut.app)
        assertEquals(entries, p.homeShortcuts)
        assertEquals(CornerShortcut(), p.rightShortcut)
        rule.onNodeWithText("Icon").performClick()
        rule.onNodeWithTag("icon-search").performTextInput("rocket")
        rule.onNodeWithContentDescription("Rocket").performClick()
        rule.onNodeWithContentDescription("Close icon picker").performClick()
        assertEquals(1, writes)
        assertEquals(ShortcutIcon.GAMING, p.leftShortcut.icon)
    }
}
