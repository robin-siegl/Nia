package dev.robinsiegl.nia

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import dev.robinsiegl.nia.data.PreferencesStore
import dev.robinsiegl.nia.model.*
import dev.robinsiegl.nia.theme.Palette
import dev.robinsiegl.nia.ui.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
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
class CommandOptionsUiTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var host: android.view.View
    @Suppress("DEPRECATION")
    private fun render(large: Boolean = false, light: Boolean = false, content: @Composable () -> Unit) {
        if (large) rule.activity.resources.let { it.updateConfiguration(Configuration(it.configuration).apply { fontScale = 2f }, it.displayMetrics) }
        val p = LauncherPreferences(themeMode = if (light) ThemeMode.LIGHT else ThemeMode.DARK)
        rule.setContent { host = LocalView.current; NiaTheme(Palette.generate(p, false)) { Surface(Modifier.fillMaxSize(), content = content) } }
    }
    private fun capture(name: String): Bitmap = rule.runOnIdle {
        val view = android.view.inspector.WindowInspector.getGlobalWindowViews().lastOrNull { it.isShown && it.width > 0 && it.height > 0 } ?: host
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap
    }
    @Test fun `Home presets edit a duration draft and preserve custom names until Save`() {
        var draft by mutableStateOf(HomeShortcutDraft(HomeShortcut("preset", ""), true))
        var saved: HomeShortcut? = null
        render { HomeShortcutEditorScreen(draft, emptyList(), false, { draft = draft.copy(shortcut = it(draft.shortcut)) },
            {}, { saved = draft.shortcut }, {}, {}) }
        rule.onNodeWithText("Command").performClick()
        rule.onNodeWithText("Preset timer").performScrollTo().performClick()
        assertEquals("5 min timer", draft.shortcut.name)
        assertEquals(ShortcutIcon.TIMER, draft.shortcut.icon)
        rule.onNodeWithText("Duration").performClick()
        capture("timer-duration-picker")
        rule.onNodeWithText("15 min").performClick()
        rule.onNodeWithText("Use duration").performClick()
        assertEquals(15, draft.shortcut.timerMinutes)
        assertEquals("15 min timer", draft.shortcut.name)
        assertNull(saved)
        rule.onNodeWithTag("home-shortcut-name").performTextReplacement("Tea")
        val before = draft.shortcut
        rule.onNodeWithText("Duration").performClick()
        rule.onNodeWithText("25 min").performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(before, draft.shortcut)
        rule.onNodeWithText("Duration").performClick()
        rule.onNodeWithTag("timer-minutes").performTextReplacement("45")
        rule.onNodeWithText("Use duration").performClick()
        assertEquals(45, draft.shortcut.timerMinutes)
        assertEquals("Tea", draft.shortcut.name)
        capture("preset-timer-editor")
        rule.onNodeWithText("Save").performScrollTo().performClick()
        assertEquals(draft.shortcut, saved)
    }
    @Test fun `both corners use the same duration picker without changing Home or writing on Cancel`() {
        val home = listOf(HomeShortcut("notes", "Notes", app = AppTarget("example.notes", ".Main")))
        var p by mutableStateOf(LauncherPreferences(homeShortcuts = home))
        var writes = 0
        render { SettingsLayout("Quick corners", {}) { QuickCornerSettings(p, emptyList(), { p = it(p); writes++ }, {}) } }
        fun corner(text: String, position: CornerPosition) = rule.onNode(hasText(text) and hasAnyAncestor(hasTestTag("corner-settings-${position.name}")))
        CornerPosition.entries.forEach { position ->
            corner("Command", position).performScrollTo().performClick()
            rule.onNode(hasText("Preset timer") and isSelectable()).performScrollTo().performClick()
            assertEquals(5, p.shortcut(position).timerMinutes)
            val previousWrites = writes
            corner("Duration", position).performScrollTo().performClick()
            rule.onNodeWithText("25 min").performClick()
            rule.onNodeWithText("Cancel").performClick()
            assertEquals(previousWrites, writes)
            assertEquals(5, p.shortcut(position).timerMinutes)
            corner("Duration", position).performScrollTo().performClick()
            rule.onNodeWithText(if (position == CornerPosition.LEFT) "15 min" else "25 min").performClick()
            rule.onNodeWithText("Use duration").performClick()
        }
        assertEquals(4, writes)
        assertEquals(15, p.leftShortcut.timerMinutes); assertEquals(25, p.rightShortcut.timerMinutes)
        assertEquals(home, p.homeShortcuts)
        corner("Empty", CornerPosition.LEFT).performScrollTo().performClick()
        assertEquals(ShortcutAction.EMPTY, p.leftShortcut.action)
        assertNull(p.leftShortcut.command); assertNull(p.leftShortcut.timerMinutes)
        assertEquals(25, p.rightShortcut.timerMinutes)
    }
    @Test @Config(qualifiers = "w320dp-h600dp-xhdpi")
    fun `custom timer limits and confirmation remain usable at large text`() {
        var selected: Int? = null
        render(large = true, light = true) { TimerDurationPicker(5, {}, { selected = it }) }
        listOf("", "0", "1441").forEach { value ->
            rule.onNodeWithTag("timer-minutes").performScrollTo().performTextReplacement(value)
            rule.onNodeWithText("Use duration").assertIsNotEnabled()
            assertNull(selected)
        }
        rule.onNodeWithTag("timer-minutes").performTextReplacement("1440")
        rule.onNodeWithText("Cancel").assertIsDisplayed()
        rule.onNodeWithText("Use duration").assertIsEnabled().assertIsDisplayed()
        capture("timer-duration-picker-large")
        rule.onNodeWithText("Use duration").performClick()
        assertEquals(1440, selected)
    }
    @Test @Config(qualifiers = "w320dp-h600dp-xhdpi")
    fun `the extended command picker reaches its last command at large text`() {
        var selected: ShortcutCommand? = null
        render(large = true, light = true) { ShortcutCommandPicker(ShortcutCommand.SET_ALARM, {}, { selected = it }) }
        rule.onNode(hasText("Set an alarm") and isSelectable()).performScrollTo().assertIsSelected()
        rule.onNodeWithText("Sound controls").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(ShortcutCommand.SOUND_CONTROLS, selected)
        capture("additional-commands-large")
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun `the real Home theme command updates the palette and system bar choice without an app launch`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app = rule.activity.application
        val store = PreferencesStore(app)
        val p = LauncherPreferences(homeShortcuts = listOf(HomeShortcut("theme", "Switch theme", ShortcutIcon.PALETTE,
            command = ShortcutCommand.TOGGLE_THEME)))
        runBlocking { store.replace(SavedState(p, true)) }
        val holder = ViewModelStore()
        val model = LauncherViewModel(app); holder.put("nia", model)
        var darkBars = false
        try {
            render { NiaApp(model, homeRequest = 0, onSystemBars = { darkBars = it }) }
            rule.waitUntil(10_000) { model.saved.value?.setupComplete == true }
            rule.onNodeWithContentDescription("Switch theme").assertIsDisplayed()
            val light = capture("command-theme-light")
            assertEquals(Color.WHITE, light.getPixel(light.width / 2, light.height - 100))
            rule.onNodeWithContentDescription("Switch theme").performTouchInput { click() }
            rule.waitUntil(10_000) { model.saved.value?.preferences?.themeMode == ThemeMode.DARK }
            rule.waitForIdle()
            val dark = capture("command-theme-dark")
            assertEquals(Color.BLACK, dark.getPixel(dark.width / 2, dark.height - 100))
            assertTrue(darkBars)
            rule.onNodeWithContentDescription("Switch theme").performTouchInput { click() }
            rule.waitUntil(10_000) { model.saved.value?.preferences?.themeMode == ThemeMode.LIGHT }
            rule.waitForIdle()
            assertFalse(darkBars)
            assertEquals(p.copy(themeMode = ThemeMode.LIGHT), model.saved.value!!.preferences)
        } finally { rule.runOnIdle { holder.clear() }; runBlocking { store.reset() }; Dispatchers.resetMain() }
    }
}
