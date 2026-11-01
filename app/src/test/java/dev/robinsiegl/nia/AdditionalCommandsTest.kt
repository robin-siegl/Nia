package dev.robinsiegl.nia

import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.content.res.Configuration
import android.provider.AlarmClock
import android.provider.Settings
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import dev.robinsiegl.nia.data.PreferencesStore
import dev.robinsiegl.nia.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class AdditionalCommandsTest {
    private val base = LauncherPreferences(clock = AppTarget("example.header", ".Main"),
        colorStyle = ColorStyle.COLOR, seedColor = "#7C3AED", pureBlackColorBackground = true)
    private fun entry(command: ShortcutCommand, minutes: Int? = null) =
        HomeShortcut(command.name, command.defaultName, command.icon, command = command, timerMinutes = minutes)
    @Suppress("DEPRECATION")
    private fun register(app: Application, action: String) {
        shadowOf(app.packageManager).addResolveInfoForIntent(Intent(action), ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "example.clock"; name = "example.clock.Handler${action.hashCode().toUInt()}"; exported = true
            }
        })
    }
    private fun assertExternal(app: Application, action: String): Intent {
        val intent = requireNotNull(shadowOf(app).nextStartedActivity)
        assertEquals(action, intent.action)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertNull(intent.component); assertNull(intent.`package`)
        return intent
    }
    private fun advance() = ShadowSystemClock.advanceBy(Duration.ofMillis(500))
    private suspend fun await(model: LauncherViewModel, expected: LauncherPreferences) =
        withTimeout(10_000) { model.saved.filterNotNull().first { it.preferences == expected } }
    private fun withModel(initial: LauncherPreferences, test: suspend (Application, LauncherViewModel) -> Unit) = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app = ApplicationProvider.getApplicationContext<Application>()
        val store = PreferencesStore(app)
        val holder = ViewModelStore()
        try {
            shadowOf(app).checkActivities(true)
            store.replace(SavedState(initial, true))
            val model = LauncherViewModel(app); holder.put("nia", model)
            await(model, initial)
            test(app, model)
        } finally { holder.clear(); store.reset(); Dispatchers.resetMain() }
    }

    @Test fun `alarm and volume commands use system screens from Home and Quick corners`() {
        val p = base.copy(homeShortcuts = listOf(entry(ShortcutCommand.SET_ALARM), entry(ShortcutCommand.SOUND_CONTROLS)),
            leftShortcut = CornerShortcut().withCommand(ShortcutCommand.SET_ALARM),
            rightShortcut = CornerShortcut().withCommand(ShortcutCommand.SOUND_CONTROLS))
        withModel(p) { app, model ->
            register(app, AlarmClock.ACTION_SET_ALARM); register(app, Settings.Panel.ACTION_VOLUME)
            p.homeShortcuts.forEach { shortcut ->
                advance(); model.openApps(); model.openHomeShortcut(shortcut.id)
                val intent = assertExternal(app, if (shortcut.command == ShortcutCommand.SET_ALARM) AlarmClock.ACTION_SET_ALARM else Settings.Panel.ACTION_VOLUME)
                if (shortcut.command == ShortcutCommand.SET_ALARM) {
                    assertFalse(intent.hasExtra(AlarmClock.EXTRA_HOUR)); assertFalse(intent.hasExtra(AlarmClock.EXTRA_MINUTES))
                    assertFalse(intent.hasExtra(AlarmClock.EXTRA_DAYS)); assertFalse(intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, true))
                }
                assertEquals(Screen.Home, model.navigation.value.screen)
            }
            CornerPosition.entries.forEach { position ->
                advance(); model.openCorner(position)
                assertExternal(app, if (position == CornerPosition.LEFT) AlarmClock.ACTION_SET_ALARM else Settings.Panel.ACTION_VOLUME)
            }
            assertEquals(p, model.saved.value!!.preferences)
        }
    }
    @Test fun `each preset sends its own duration including the supported endpoints`() {
        val entries = listOf(1, 5, 15, 25, 1440).map { entry(ShortcutCommand.PRESET_TIMER, it).copy(id = "preset-$it") }
        val p = base.copy(homeShortcuts = entries,
            leftShortcut = CornerShortcut().withCommand(ShortcutCommand.PRESET_TIMER).copy(timerMinutes = 15),
            rightShortcut = CornerShortcut().withCommand(ShortcutCommand.PRESET_TIMER).copy(timerMinutes = 25))
        withModel(p) { app, model ->
            register(app, AlarmClock.ACTION_SET_TIMER)
            entries.forEach { shortcut ->
                advance(); model.openHomeShortcut(shortcut.id)
                val intent = assertExternal(app, AlarmClock.ACTION_SET_TIMER)
                assertEquals(shortcut.timerMinutes!! * 60, intent.getIntExtra(AlarmClock.EXTRA_LENGTH, -1))
                assertFalse(intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, true))
                model.openHomeShortcut(shortcut.id)
                assertNull("A duplicate tap must not start a second timer", shadowOf(app).nextStartedActivity)
            }
            CornerPosition.entries.forEach { position ->
                advance(); model.openCorner(position)
                assertEquals(p.shortcut(position).timerMinutes!! * 60,
                    assertExternal(app, AlarmClock.ACTION_SET_TIMER).getIntExtra(AlarmClock.EXTRA_LENGTH, -1))
            }
            assertEquals(p, model.saved.value!!.preferences)
        }
    }
    @Test fun `a missing volume panel falls back to Android sound settings`() = withModel(
        base.copy(homeShortcuts = listOf(entry(ShortcutCommand.SOUND_CONTROLS)))) { app, model ->
        register(app, Settings.ACTION_SOUND_SETTINGS)
        model.openHomeShortcut(ShortcutCommand.SOUND_CONTROLS.name)
        assertExternal(app, Settings.ACTION_SOUND_SETTINGS)
        assertEquals(Screen.Home, model.navigation.value.screen)
    }
    @Test fun `unavailable external commands preserve settings and allow immediate retries`() {
        val p = base.copy(homeShortcuts = listOf(entry(ShortcutCommand.SET_ALARM), entry(ShortcutCommand.PRESET_TIMER, 5), entry(ShortcutCommand.SOUND_CONTROLS)))
        withModel(p) { app, model ->
            val cases = listOf(Triple(ShortcutCommand.SET_ALARM, AlarmClock.ACTION_SET_ALARM, "No alarm app"),
                Triple(ShortcutCommand.PRESET_TIMER, AlarmClock.ACTION_SET_TIMER, "No timer app"),
                Triple(ShortcutCommand.SOUND_CONTROLS, Settings.Panel.ACTION_VOLUME, "Sound controls could not"))
            cases.forEach { (command, action, feedback) ->
                advance(); model.openApps(); model.openHomeShortcut(command.name)
                assertTrue(withTimeout(1_000) { model.messages.first() }.startsWith(feedback))
                assertEquals(Screen.Apps, model.navigation.value.screen)
                assertEquals(p, model.saved.value!!.preferences)
                shadowOf(app).clearNextStartedActivities()
                register(app, action)
                model.openHomeShortcut(command.name)
                assertExternal(app, action)
                assertEquals(Screen.Home, model.navigation.value.screen)
            }
        }
    }
    @Test fun `appearance reset keeps presets and replacing them with apps clears duration parameters`() {
        val p = base.copy(homeShortcuts = listOf(entry(ShortcutCommand.PRESET_TIMER, 25)),
            leftShortcut = CornerShortcut().withCommand(ShortcutCommand.PRESET_TIMER).copy(timerMinutes = 15),
            rightShortcut = CornerShortcut().withCommand(ShortcutCommand.PRESET_TIMER))
        withModel(p) { _, model ->
            model.resetAppearance()
            val reset = LauncherPreferences(homeShortcuts = p.homeShortcuts, clock = p.clock,
                leftShortcut = p.leftShortcut, rightShortcut = p.rightShortcut)
            await(model, reset)
            val app = AppEntry(AppTarget("example.notes", ".Main"), "Notes")
            model.editHomeShortcut(ShortcutCommand.PRESET_TIMER.name)
            model.navigate(Screen.HomeShortcutPicker)
            model.chooseHomeApp(app)
            assertNull(model.homeDraft.value!!.shortcut.command)
            assertNull(model.homeDraft.value!!.shortcut.timerMinutes)
            model.saveHomeShortcut()
            val homeAssigned = reset.copy(homeShortcuts = listOf(reset.homeShortcuts.single().copy(app = app.target, command = null, timerMinutes = null)))
            await(model, homeAssigned)
            withTimeout(10_000) { model.selectingApp.first { !it } }
            model.chooseCornerApp(CornerPosition.LEFT, app.target)
            val leftAssigned = homeAssigned.copy(leftShortcut = homeAssigned.leftShortcut.copy(action = ShortcutAction.APP,
                app = app.target, command = null, timerMinutes = null))
            await(model, leftAssigned)
            model.assignCornerFromApps(CornerPosition.RIGHT, app)
            val bothAssigned = leftAssigned.copy(rightShortcut = leftAssigned.rightShortcut.copy(action = ShortcutAction.APP,
                app = app.target, command = null, timerMinutes = null))
            await(model, bothAssigned)
        }
    }
    private fun themeRoundTrip(systemDark: Boolean) {
        val p = base.copy(homeShortcuts = listOf(entry(ShortcutCommand.TOGGLE_THEME)),
            leftShortcut = CornerShortcut().withCommand(ShortcutCommand.TOGGLE_THEME),
            rightShortcut = CornerShortcut().withCommand(ShortcutCommand.TOGGLE_THEME))
        withModel(p) { app, model ->
            assertEquals(systemDark, app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
            model.openHomeShortcut(ShortcutCommand.TOGGLE_THEME.name)
            val first = p.copy(themeMode = if (systemDark) ThemeMode.LIGHT else ThemeMode.DARK)
            await(model, first)
            model.openCorner(CornerPosition.LEFT)
            val second = p.copy(themeMode = if (systemDark) ThemeMode.DARK else ThemeMode.LIGHT)
            await(model, second)
            model.openCorner(CornerPosition.RIGHT)
            await(model, first)
            assertEquals(Screen.Home, model.navigation.value.screen)
            assertNull(shadowOf(app).nextStartedActivity)
            assertEquals(systemDark, app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
            val holder = ViewModelStore()
            try {
                val restarted = LauncherViewModel(app); holder.put("restart", restarted)
                await(restarted, first)
                restarted.updatePreferences { it.copy(themeMode = ThemeMode.SYSTEM) }
                await(restarted, p)
            } finally { holder.clear() }
        }
    }
    @Test fun `theme toggles persist from a light System theme without changing colors or Android`() = themeRoundTrip(false)
    @Test @Config(qualifiers = "night")
    fun `theme toggles persist from a dark System theme without changing colors or Android`() = themeRoundTrip(true)
}
