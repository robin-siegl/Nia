package dev.robinsiegl.nia

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.provider.AlarmClock
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
class SystemTimerCommandTest {
    private val preferences = LauncherPreferences(
        clock = AppTarget("example.other", ".ChosenHeaderApp"),
        homeShortcuts = listOf(HomeShortcut("timer", "Tea", ShortcutIcon.TIMER, command = ShortcutCommand.START_TIMER)),
        leftShortcut = CornerShortcut(ShortcutAction.COMMAND, ShortcutIcon.TIMER, command = ShortcutCommand.START_TIMER),
        rightShortcut = CornerShortcut(ShortcutAction.COMMAND, ShortcutIcon.STAR, command = ShortcutCommand.START_TIMER),
    )

    private fun registerTimer(app: Application) {
        shadowOf(app.packageManager).addResolveInfoForIntent(Intent(AlarmClock.ACTION_SET_TIMER), ResolveInfo().apply {
            activityInfo = ActivityInfo().apply { packageName = "example.clock"; name = "example.clock.TimerActivity"; exported = true }
        })
    }

    private fun assertTimer(intent: Intent?) {
        assertNotNull(intent)
        requireNotNull(intent)
        assertEquals(AlarmClock.ACTION_SET_TIMER, intent.action)
        assertFalse("The user chooses a duration in the Clock app", intent.hasExtra(AlarmClock.EXTRA_LENGTH))
        assertFalse(intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, true))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertNull("Android chooses a compatible timer app", intent.component)
        assertNull(intent.`package`)
    }

    private fun withModel(test: suspend (Application, LauncherViewModel) -> Unit) = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app = ApplicationProvider.getApplicationContext<Application>()
        val store = PreferencesStore(app)
        val holder = ViewModelStore()
        try {
            shadowOf(app).checkActivities(true)
            store.replace(SavedState(preferences, true))
            val model = LauncherViewModel(app)
            holder.put("nia", model)
            withTimeout(10_000) { model.saved.filterNotNull().first { it.preferences == preferences } }
            test(app, model)
        } finally { holder.clear(); store.reset(); Dispatchers.resetMain() }
    }

    @Test fun `Home and both corners open the system timer setup without starting a preset`() = withModel { app, model ->
        registerTimer(app)
        val permissions = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertTrue(Manifest.permission.SET_ALARM in permissions)
        assertFalse(Manifest.permission.SCHEDULE_EXACT_ALARM in permissions)
        assertFalse(Manifest.permission.USE_EXACT_ALARM in permissions)
        model.openApps()
        model.openHomeShortcut("timer")
        assertTimer(shadowOf(app).nextStartedActivity)
        assertEquals(Screen.Home, model.navigation.value.screen)
        model.openCorner(CornerPosition.LEFT)
        assertNull("Rapid duplicate external launches stay blocked", shadowOf(app).nextStartedActivity)
        CornerPosition.entries.forEach { position ->
            ShadowSystemClock.advanceBy(Duration.ofMillis(500))
            model.openCorner(position)
            assertTimer(shadowOf(app).nextStartedActivity)
        }
        assertEquals(preferences, model.saved.value!!.preferences)
    }

    @Test fun `a missing timer app preserves navigation and a successful retry is not blocked`() = withModel { app, model ->
        model.openApps()
        model.openHomeShortcut("timer")
        val message = withTimeout(1_000) { model.messages.first() }
        assertEquals("No timer app could be opened. Install or enable a Clock app and try again.", message)
        assertEquals(Screen.Apps, model.navigation.value.screen)
        assertEquals(preferences, model.saved.value!!.preferences)
        shadowOf(app).clearNextStartedActivities()
        registerTimer(app)
        // No delay: a failed external launch must release the shared launch gate.
        model.openCorner(CornerPosition.RIGHT)
        assertTimer(shadowOf(app).nextStartedActivity)
        assertEquals(Screen.Home, model.navigation.value.screen)
    }
}
