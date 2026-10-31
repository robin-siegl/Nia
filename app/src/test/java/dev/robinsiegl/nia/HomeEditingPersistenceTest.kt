package dev.robinsiegl.nia

import android.app.Application
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class HomeEditingPersistenceTest {
    @Test fun `real editor actions persist one ordered Home and preserve independent header apps`() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app = ApplicationProvider.getApplicationContext<Application>()
        val store = PreferencesStore(app)
        val holder = ViewModelStore()
        val calendar = AppTarget("example.calendar", ".Main")
        val clock = AppTarget("example.clock", ".Main")
        val maps = AppEntry(AppTarget("example.maps", ".Main"), "Google Maps")
        suspend fun await(model: LauncherViewModel, predicate: (LauncherPreferences) -> Boolean): LauncherPreferences =
            withTimeout(10_000) { model.saved.filterNotNull().first { predicate(it.preferences) }.preferences }
        try {
            store.replace(SavedState(LauncherPreferences(layout = HomeLayout.LIST, clock = clock, calendar = calendar,
                homeShortcuts = legacyHomeShortcuts(mapOf(HomeCategory.CALENDAR to calendar))), setupComplete = true))
            val model = LauncherViewModel(app)
            holder.put("nia", model)
            await(model) { it.homeShortcuts.size == 5 }
            model.navigate(Screen.Settings)
            model.editHomeShortcut("legacy-maps")
            model.updateHomeDraft { it.copy(name = "My maps", icon = ShortcutIcon.MAPS) }
            model.navigate(Screen.HomeShortcutPicker)
            model.chooseHomeApp(maps)
            // A second callback from the departing picker cannot pop or overwrite the editor.
            model.chooseHomeApp(maps.copy(label = "Unexpected second callback"))
            assertEquals(Screen.HomeShortcutEditor, model.navigation.value.screen)
            assertEquals("My maps", model.homeDraft.value!!.shortcut.name)
            model.saveHomeShortcut()
            await(model) { it.homeShortcuts.last().app == maps.target }
            model.moveHomeShortcut("legacy-maps", -1)
            await(model) { it.homeShortcuts[3].id == "legacy-maps" }
            model.removeHomeShortcut("legacy-calendar")
            await(model) { it.homeShortcuts.none { entry -> entry.id == "legacy-calendar" } }
            assertEquals(calendar, model.saved.value!!.preferences.calendar)
            val entries = model.saved.value!!.preferences.homeShortcuts
            model.resetAppearance()
            val reset = await(model) { it.layout == HomeLayout.GRID }
            assertEquals(entries, reset.homeShortcuts)
            assertEquals(clock, reset.clock); assertEquals(calendar, reset.calendar)
            model.editHomeShortcut()
            model.updateHomeDraft { it.copy(name = "Discard me") }
            model.back()
            assertNull(model.homeDraft.value)
            assertEquals(entries, model.saved.value!!.preferences.homeShortcuts)
            model.updatePreferences { it.copy(homeShortcuts = (1..8).map { i -> HomeShortcut("app-$i", "App $i", app = maps.target) }) }
            await(model) { it.homeShortcuts.size == 8 }
            model.editHomeShortcut()
            assertNull(model.homeDraft.value)
            model.updatePreferences { it.copy(homeShortcuts = emptyList()) }
            await(model) { it.homeShortcuts.isEmpty() }
            holder.clear()
            val restarted = LauncherViewModel(app)
            holder.put("nia", restarted)
            val persisted = await(restarted) { it.homeShortcuts.isEmpty() }
            assertEquals(calendar, persisted.calendar)
            assertEquals(clock, persisted.clock)
            assertTrue(restarted.saved.value!!.setupComplete)
            assertEquals(persisted, withTimeout(10_000) { store.state.first() }.preferences)
        } finally {
            holder.clear()
            store.reset()
            Dispatchers.resetMain()
        }
    }
}
