package dev.robinsiegl.nia

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
class ShortcutActionsUiTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var host: android.view.View
    private val notes = AppEntry(AppTarget("example.notes", ".Main"), "Notes")
    @Suppress("DEPRECATION")
    private fun render(light: Boolean = false, large: Boolean = false, content: @Composable () -> Unit) {
        if (large) rule.activity.resources.let { it.updateConfiguration(Configuration(it.configuration).apply { fontScale = 2f }, it.displayMetrics) }
        val p = LauncherPreferences(themeMode = if (light) ThemeMode.LIGHT else ThemeMode.DARK)
        rule.setContent { host = LocalView.current; NiaTheme(Palette.generate(p, false)) { Surface(Modifier.fillMaxSize(), content = content) } }
    }
    private fun capture(name: String) = rule.runOnIdle {
        val root = android.view.inspector.WindowInspector.getGlobalWindowViews().lastOrNull { it.isShown && it.width > 0 && it.height > 0 } ?: host
        val bitmap = android.graphics.Bitmap.createBitmap(root.width, root.height, android.graphics.Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bitmap))
        File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }.outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    @Test fun `app menu adds to Home through its own action without launching and keeps corner actions`() {
        var added: AppEntry? = null
        var launches = 0
        render { AppListScreen(listOf(notes), false, false, onBack = {}, onSelect = { launches++ }, onInfo = {},
            onAssignCorner = { _, _ -> }, onAddHome = { added = it }) }
        rule.onNodeWithText("Notes").performTouchInput { longClick() }
        rule.onNodeWithText("Add to Home").assertIsDisplayed()
        rule.onNodeWithText("App info").assertIsDisplayed()
        rule.onNodeWithText("Left quick corner").assertIsDisplayed()
        rule.onNodeWithText("Right quick corner").assertIsDisplayed()
        capture("app-actions")
        rule.onNodeWithText("Add to Home").performClick()
        assertEquals(notes, added); assertEquals(0, launches)
        rule.onNodeWithTag("app-actions-menu").assertDoesNotExist()
    }
    @Test @Config(qualifiers = "w320dp-h600dp-xhdpi")
    fun `full Home feedback remains readable with large text and leaves App info enabled`() {
        var additions = 0; var info = 0
        render(light = true, large = true) { AppListScreen(listOf(notes), false, false, onBack = {}, onSelect = {},
            onInfo = { info++ }, onAssignCorner = { _, _ -> }, onAddHome = { additions++ }, canAddHome = false) }
        rule.onNodeWithText("Notes").performTouchInput { longClick() }
        rule.onNodeWithText("Add to Home").assertIsNotEnabled()
        rule.onNodeWithText("Home already has nine shortcuts").assertIsDisplayed()
        rule.onNodeWithText("App info").assertIsEnabled().performClick()
        assertEquals(1, info); assertEquals(0, additions)
    }
    @Test fun `a command draft preserves a custom name and only saves after confirmation`() {
        var draft by mutableStateOf(HomeShortcutDraft(HomeShortcut("new", ""), true))
        var saved: HomeShortcut? = null
        render { HomeShortcutEditorScreen(draft, listOf(notes), false,
            { draft = draft.copy(shortcut = it(draft.shortcut)) }, {}, { saved = draft.shortcut }, {}, {}) }
        rule.onNodeWithTag("home-shortcut-name").performTextInput("Find something")
        rule.onNodeWithText("Command").performClick()
        rule.onNodeWithTag("home-shortcut-name").assertIsNotFocused()
        capture("shortcut-command-picker")
        rule.onNodeWithText("Search apps").performClick()
        assertNull(saved)
        assertEquals("Find something", draft.shortcut.name)
        assertEquals(ShortcutCommand.SEARCH_APPS, draft.shortcut.command)
        assertNull(draft.shortcut.app)
        rule.onNodeWithText("Save").performScrollTo().performClick()
        assertEquals(draft.shortcut, saved)
    }
    @Test fun `canceling command selection leaves the app name icon and target untouched`() {
        val original = HomeShortcut("notes", "My notes", ShortcutIcon.NOTES, notes.target)
        var draft by mutableStateOf(HomeShortcutDraft(original, false))
        render { HomeShortcutEditorScreen(draft, listOf(notes), false,
            { draft = draft.copy(shortcut = it(draft.shortcut)) }, {}, {}, {}, {}) }
        rule.onNodeWithText("Command").performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(original, draft.shortcut)
        rule.onNodeWithText("Opens").assertIsDisplayed()
    }
    @Test fun `Quick corners can choose the same commands and icon picker without changing Home`() {
        val home = listOf(HomeShortcut("notes", "Notes", app = notes.target))
        var p by mutableStateOf(LauncherPreferences(homeShortcuts = home))
        var writes = 0
        render { SettingsLayout("Quick corners", {}) {
            QuickCornerSettings(p, listOf(notes), { p = it(p); writes++ }, {})
        } }
        rule.onNode(hasText("Command") and hasAnyAncestor(hasTestTag("corner-settings-LEFT"))).performClick()
        rule.onNodeWithText("Toggle flashlight").performClick()
        assertEquals(1, writes)
        assertEquals(ShortcutAction.COMMAND, p.leftShortcut.action)
        assertEquals(ShortcutCommand.FLASHLIGHT, p.leftShortcut.command)
        assertEquals(ShortcutIcon.FLASHLIGHT, p.leftShortcut.icon)
        assertNull(p.leftShortcut.app)
        rule.onNodeWithText("Icon").performClick()
        rule.onNodeWithTag("icon-search").performTextInput("rocket")
        rule.onNodeWithContentDescription("Rocket").performClick()
        rule.onNodeWithText("Use icon").performClick()
        assertEquals(2, writes)
        assertEquals(ShortcutIcon.ROCKET, p.leftShortcut.icon)
        assertEquals(ShortcutCommand.FLASHLIGHT, p.leftShortcut.command)
        assertEquals(home, p.homeShortcuts)
        assertEquals(CornerShortcut(), p.rightShortcut)
    }
    @Test @Config(qualifiers = "w320dp-h600dp-xhdpi")
    fun `command selection stays visible and reachable with large text`() {
        var chosen: ShortcutCommand? = null
        render(light = true, large = true) {
            ShortcutCommandPicker(ShortcutCommand.OPEN_APPS, {}, { chosen = it })
        }
        rule.onNode(hasText("Open Apps") and isSelectable()).assertIsSelected()
        rule.onNodeWithText("Toggle flashlight").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(ShortcutCommand.FLASHLIGHT, chosen)
        capture("shortcut-command-picker-large")
    }
}
