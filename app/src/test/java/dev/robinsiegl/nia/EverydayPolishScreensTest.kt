package dev.robinsiegl.nia

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
class EverydayPolishScreensTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var host: android.view.View
    private val notes = AppEntry(AppTarget("example.notes", ".Main"), "Notes")
    private val mail = AppEntry(AppTarget("example.mail", ".Main"), "Mail")
    private val release = AppRelease(ReleaseVersion(1, 3, 0), "${AppRelease.REPOSITORY}/releases/tag/v1.3.0",
        "${AppRelease.REPOSITORY}/releases/download/v1.3.0/nia-1.3.0.apk")

    private fun render(content: @Composable () -> Unit) {
        rule.setContent {
            host = LocalView.current
            NiaTheme(Palette.generate(LauncherPreferences(themeMode = ThemeMode.DARK), true)) {
                Surface(Modifier.fillMaxSize(), content = content)
            }
        }
    }
    private fun capture(name: String) = rule.runOnIdle {
        // Dialogs/popups have their own Android windows; drawing only the host omits them.
        val root = android.view.inspector.WindowInspector.getGlobalWindowViews().lastOrNull {
            it.isShown && it.width > 0 && it.height > 0
        }?.let { if (it === host.rootView) host else it } ?: host
        val bitmap = android.graphics.Bitmap.createBitmap(root.width, root.height, android.graphics.Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bitmap))
        File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }.outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun `long press offers app actions without launching or opening info immediately`() {
        var launches = 0
        var infos = 0
        val assigned = mutableListOf<Pair<AppEntry, CornerPosition>>()
        render { AppListScreen(listOf(mail, notes), false, false, onBack = {}, onSelect = { launches++ },
            onInfo = { infos++ }, onAssignCorner = { app, position -> assigned += app to position }) }
        rule.onNodeWithText("Notes").performTouchInput { longClick() }
        assertEquals(0, launches); assertEquals(0, infos)
        rule.onNodeWithText("App info").assertIsDisplayed()
        rule.onNodeWithText("Left quick corner").assertIsDisplayed()
        rule.onNodeWithText("Right quick corner").assertIsDisplayed()
        capture("app-actions-basic")
        rule.onNodeWithText("App info").performClick()
        assertEquals(1, infos)
        rule.onNodeWithText("Notes").performTouchInput { longClick() }
        rule.onNodeWithText("Left quick corner").performClick()
        rule.onNodeWithText("Mail").performTouchInput { longClick() }
        rule.onNodeWithText("Right quick corner").performClick()
        assertEquals(listOf(notes to CornerPosition.LEFT, mail to CornerPosition.RIGHT), assigned)
        assertEquals(0, launches)
    }

    @Test fun `transition disables and dismisses an open app menu`() {
        var ready by mutableStateOf(true)
        render { AppListScreen(listOf(notes), false, false, onBack = {}, onSelect = {}, onInfo = {}, interactionEnabled = ready) }
        rule.onNodeWithText("Notes").performTouchInput { longClick() }
        rule.onNodeWithText("App info").assertIsDisplayed()
        rule.runOnIdle { ready = false }
        rule.onNodeWithText("App info").assertDoesNotExist()
    }

    @Test fun `manual update status checks only on tap and opens the exact APK link`() {
        var checks = 0
        var state by mutableStateOf<UpdateCheck>(UpdateCheck.Idle)
        var opened: String? = null
        render { Column { UpdateContent(state, { checks++; state = UpdateCheck.Checking }, { opened = it }, "1.2.0") } }
        assertEquals(0, checks)
        rule.onNodeWithText("Check for updates").performClick()
        assertEquals(1, checks)
        rule.onNodeWithText("Checking…").assertIsNotEnabled()
        rule.runOnIdle { state = UpdateCheck.Result(release) }
        rule.onNodeWithText("Update available: 1.3.0").assertIsDisplayed()
        rule.onNodeWithText("Download 1.3.0 APK").performClick()
        assertEquals(release.apkUrl, opened)
        rule.onNodeWithText("View release on GitHub").performClick()
        assertEquals(release.pageUrl, opened)
    }

    @Test fun `same version is up to date and missing APK still has a release page`() {
        render { Column { UpdateContent(UpdateCheck.Result(release.copy(apkUrl = null)), {}, {}, "1.3.0-debug") } }
        rule.onNodeWithText("You’re up to date. Latest release: 1.3.0").assertIsDisplayed()
        rule.onNodeWithText("Download 1.3.0 APK").assertDoesNotExist()
        rule.onNodeWithText("View release on GitHub").assertIsDisplayed()
    }

    @Test fun `failed check offers retry and a browser fallback`() {
        var checks = 0
        var link: String? = null
        render { Column { UpdateContent(UpdateCheck.Failed("Check your connection."), { checks++ }, { link = it }) } }
        rule.onNodeWithText("Check your connection.").assertIsDisplayed()
        rule.onNodeWithText("Check again").performClick()
        assertEquals(1, checks)
        rule.onNodeWithText("View releases on GitHub").performClick()
        assertEquals(ProjectLinks.RELEASES, link)
    }

    @Test fun `restore preview shows exact changes and cancellation cannot apply them`() {
        val p = LauncherPreferences()
        val requested = p.copy(layout = HomeLayout.LIST, searchAutofocus = true,
            homeShortcuts = listOf(HomeShortcut("music", "Music", ShortcutIcon.MUSIC, notes.target)))
        val preview = RestorePreview.create(p, requested, listOf(notes))
        var restores = 0
        var cancels = 0
        render { RestoreDialog(preview, false, { restores++ }, { cancels++ }) }
        rule.onNodeWithText("4 changes from this backup.").assertIsDisplayed()
        rule.onNodeWithText("Current: Icon grid").assertIsDisplayed()
        rule.onNodeWithText("Backup: Text list").assertIsDisplayed()
        rule.onNodeWithText("Backup: Music · Music icon · Notes · example.notes").performScrollTo().assertIsDisplayed()
        capture("restore-preview")
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(1, cancels); assertEquals(0, restores)
        rule.onNodeWithText("Restore").performClick()
        assertEquals(1, restores)
    }

    @Test fun `restore progress disables duplicate submission and cancel`() {
        render { RestoreDialog(RestorePreview.create(LauncherPreferences(), LauncherPreferences(), emptyList()), true, {}, {}) }
        rule.onNodeWithText("Restoring…").assertIsNotEnabled()
        rule.onNodeWithText("Cancel").assertIsNotEnabled()
        rule.onNodeWithText("No settings will change.").assertIsDisplayed()
    }

    @Test fun `large text restore can reach the final assignment and keeps its buttons visible`() {
        val target = notes.target
        val requested = LauncherPreferences(layout = HomeLayout.LIST, searchAutofocus = true,
            homeShortcuts = legacyHomeShortcuts(HomeCategory.entries.associateWith { target }),
            rightShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.TRAIN, target))
        render {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                RestoreDialog(RestorePreview.create(LauncherPreferences(), requested, listOf(notes)), false, {}, {})
            }
        }
        rule.onNodeWithText("Right icon").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Backup: Train").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Restore").assertIsDisplayed()
        rule.onNodeWithText("Cancel").assertIsDisplayed()
        capture("restore-large-text")
    }

    @Test fun `corner icon picker scrolls through the extended collection without a Settings action`() {
        var p by mutableStateOf(LauncherPreferences(leftShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.STAR, notes.target)))
        render { Column { QuickCornerSettings(p, listOf(notes), { p = it(p) }, {}) } }
        rule.onNodeWithText("Settings", substring = false).assertDoesNotExist()
        rule.onNodeWithText("Icon").performClick()
        capture("shortcut-icons")
        rule.onNodeWithTag("shortcut-icon-grid").performScrollToNode(hasContentDescription("Cloud"))
        rule.onNodeWithContentDescription("Cloud").performClick()
        rule.onNodeWithText("Use icon").performClick()
        assertEquals(ShortcutIcon.CLOUD, p.leftShortcut.icon)
        assertEquals(notes.target, p.leftShortcut.app)
        assertEquals(193, ShortcutIcon.entries.size)
    }

    @Test fun `pending unlock animates on delayed Home composition and never replays after remount`() {
        val event = UnlockRevealRequest()
        var show by mutableStateOf(true)
        render { if (show) HomeScreen(HomeLayout.GRID, "14:03", "Fri, 30 Oct", {}, {}, unlockRequest = event, shortcuts = legacyHomeShortcuts()) }
        rule.mainClock.autoAdvance = false
        // Compose may run before frame-clock control is paused; use a fresh event for the explicit transition below.
        rule.mainClock.advanceTimeBy(1000)
        assertFalse(event.pending)
        rule.runOnIdle { show = false; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle { show = true; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Music").assertIsEnabled()
    }

    @Test fun `unlock frames keep the clock and date fixed during the delayed shortcut reveal`() {
        var locked by mutableStateOf(true)
        var event by mutableStateOf<UnlockRevealRequest?>(null)
        render { HomeScreen(HomeLayout.GRID, "14:03", "Fri, 30 Oct", {}, {}, unlockRequest = event, deviceLocked = locked, shortcuts = legacyHomeShortcuts()) }
        val clock = rule.onNodeWithTag("home-clock").fetchSemanticsNode().boundsInRoot
        rule.mainClock.autoAdvance = false
        capture("unlock-motion-00")
        rule.runOnIdle { locked = false; event = UnlockRevealRequest(); Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(80)
        rule.onNodeWithContentDescription("Music").assertIsNotEnabled()
        for (frame in 1..20) {
            rule.mainClock.advanceTimeBy(40)
            assertEquals(clock, rule.onNodeWithTag("home-clock").fetchSemanticsNode().boundsInRoot)
            capture("unlock-motion-" + frame.toString().padStart(2, '0'))
        }
        rule.onNodeWithContentDescription("Music").assertIsEnabled()
    }
}
