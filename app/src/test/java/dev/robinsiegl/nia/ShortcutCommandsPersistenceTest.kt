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
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class ShortcutCommandsPersistenceTest {
    @Test fun `real Add to Home commands and drag saves preserve preferences across restart`() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app = ApplicationProvider.getApplicationContext<Application>()
        val store = PreferencesStore(app)
        val holder = ViewModelStore()
        val maps = AppEntry(AppTarget("example.maps", ".Main"), "Google Maps")
        val initial = LauncherPreferences(homeShortcuts = listOf(appsCommand(),
            HomeShortcut("flash", "Light", ShortcutIcon.FLASHLIGHT, command = ShortcutCommand.FLASHLIGHT)),
            rightShortcut = CornerShortcut(ShortcutAction.COMMAND, ShortcutIcon.SEARCH, command = ShortcutCommand.SEARCH_APPS))
        suspend fun await(model: LauncherViewModel, predicate: (LauncherPreferences) -> Boolean) =
            withTimeout(10_000) { model.saved.filterNotNull().first { predicate(it.preferences) }.preferences }
        try {
            store.replace(SavedState(initial, true))
            val model = LauncherViewModel(app); holder.put("nia", model)
            await(model) { it == initial }
            model.openApps()
            model.addHomeFromApps(maps)
            assertEquals(maps.target, model.homeDraft.value!!.shortcut.app)
            assertEquals("Google Maps", model.homeDraft.value!!.shortcut.name)
            model.back()
            assertNull(model.homeDraft.value)
            assertEquals(initial, model.saved.value!!.preferences)
            model.addHomeFromApps(maps)
            model.saveHomeShortcut()
            await(model) { it.homeShortcuts.size == 3 }
            withTimeout(10_000) { model.navigation.first { it.screen == Screen.Home } }
            val ids = model.saved.value!!.preferences.homeShortcuts.map { it.id }.reversed()
            assertTrue(model.reorderHomeShortcuts(ids))
            val reordered = await(model) { it.homeShortcuts.map { entry -> entry.id } == ids }
            assertFalse(model.reorderHomeShortcuts(ids.dropLast(1)))
            assertEquals(reordered, model.saved.value!!.preferences)
            model.openHomeShortcut("apps-command")
            assertEquals(Screen.Apps, model.navigation.value.screen)
            assertFalse(model.searchAppsOnOpen.value)
            model.back()
            ShadowSystemClock.advanceBy(Duration.ofMillis(500))
            model.openCorner(CornerPosition.RIGHT)
            assertEquals(Screen.Apps, model.navigation.value.screen)
            assertTrue(model.searchAppsOnOpen.value)
            assertFalse(model.saved.value!!.preferences.searchAutofocus)
            model.home()
            assertFalse(model.searchAppsOnOpen.value)
            model.assignCornerFromApps(CornerPosition.RIGHT, maps)
            val assigned = await(model) { it.rightShortcut.app == maps.target }
            assertEquals(ShortcutAction.APP, assigned.rightShortcut.action)
            assertNull(assigned.rightShortcut.command)
            assertEquals(ShortcutIcon.SEARCH, assigned.rightShortcut.icon)
            holder.clear()
            val restarted = LauncherViewModel(app); holder.put("nia", restarted)
            assertEquals(assigned, await(restarted) { it.homeShortcuts.map { entry -> entry.id } == ids })
            assertFalse(restarted.searchAppsOnOpen.value)
            restarted.updatePreferences { it.copy(homeShortcuts = (1..9).map { i -> HomeShortcut("id-$i", "App $i", app = maps.target) }) }
            await(restarted) { it.homeShortcuts.size == 9 }
            restarted.addHomeFromApps(maps)
            assertNull(restarted.homeDraft.value)
        } finally { holder.clear(); store.reset(); Dispatchers.resetMain() }
    }
}
