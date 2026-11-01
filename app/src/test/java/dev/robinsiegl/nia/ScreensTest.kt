package dev.robinsiegl.nia

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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

    @Composable
    private fun detail(section: SettingsSection, preferences: LauncherPreferences = LauncherPreferences(),
        onBack: () -> Unit = {}, onChange: ((LauncherPreferences) -> LauncherPreferences) -> Unit = {},
        onResetAll: () -> Unit = {}, onOpenLink: (String) -> Unit = {}) {
        SettingsDetailScreen(section, preferences, emptyList(), false, onBack, onChange,
            {}, {}, {}, {}, {}, onResetAll, onOpenLink)
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
        var selected: String? = null
        render { HomeScreen(HomeLayout.GRID, "09:41", "Friday, October 30", {}, { selected = it }, shortcuts = legacyHomeShortcuts()) }
        HomeCategory.entries.forEach { rule.onNodeWithContentDescription(it.label).assertIsDisplayed() }
        rule.onNodeWithText("Music").assertDoesNotExist()
        rule.onNodeWithText("Apps").assertDoesNotExist()
        rule.onNodeWithContentDescription("Apps").assertDoesNotExist()
        screenshot("home-oled-grid")
        rule.onNodeWithContentDescription("Music").performClick()
        assertEquals("legacy-music", selected)
    }

    @Test fun `text home shows only the configured shortcuts`() {
        var opened = false
        render(LauncherPreferences(themeMode = ThemeMode.LIGHT)) {
            HomeScreen(HomeLayout.LIST, "09:41", "Friday, October 30", {}, { opened = it == "legacy-music" }, shortcuts = legacyHomeShortcuts())
        }
        HomeCategory.entries.forEach { rule.onNodeWithText(it.label).assertIsDisplayed() }
        assertEquals(rule.onNodeWithTag("home-entry-legacy-music").fetchSemanticsNode().boundsInRoot.height,
            rule.onNodeWithTag("home-entry-legacy-maps").fetchSemanticsNode().boundsInRoot.height, 0f)
        screenshot("home-light-list")
        rule.onNodeWithText("Music").performClick()
        assertTrue(opened)
    }

    @Test fun `Apps icon and optional app corner open their destinations`() {
        var openedApps = false
        var openedCorner = false
        render {
            HomeScreen(HomeLayout.GRID, "09:41", "Friday, October 30", {}, { openedApps = it == "apps-command" },
                rightShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.CAMERA, AppTarget("example.camera", ".Main")),
                onCorner = { openedCorner = true }, cornerLabel = { "Camera" }, shortcuts = legacyHomeShortcuts() + appsCommand())
        }
        rule.onNodeWithContentDescription("Apps").performClick()
        rule.onNodeWithContentDescription("Camera").performClick()
        assertTrue(openedApps && openedCorner)
    }

    @Test @Config(qualifiers = "w320dp-h600dp-xhdpi")
    fun `large Home text and system font scaling keep Apps and corner shortcuts reachable`() {
        var opened = false
        render {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                HomeScreen(HomeLayout.LIST, "09:41", "Friday, October 30", {}, { opened = it == "apps-command" },
                    textSize = HomeTextSize.LARGE,
                    rightShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.CAMERA, AppTarget("example.camera", ".Main")),
                    cornerLabel = { "Camera" }, shortcuts = legacyHomeShortcuts() + appsCommand())
            }
        }
        rule.onNodeWithContentDescription("Camera").assertIsDisplayed()
        rule.onNodeWithText("Apps").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(opened)
        screenshot("home-large-system-text")
    }

    @Test fun `swipe up opens Apps`() {
        var opened by mutableStateOf(false)
        render {
            HomeAppsScreen(LauncherPreferences(), emptyList(), false, "09:41", "Friday, October 30", opened,
                { opened = it }, {}, {}, {}, {}, {}, {})
        }
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

    @Test fun `app list replaces settings text with a gear`() {
        var opened = false
        render { AppListScreen(emptyList(), false, false, onBack = {}, onSelect = {}, onSettings = { opened = true }) }
        rule.onNodeWithText("nia settings").assertDoesNotExist()
        rule.onNodeWithContentDescription("Settings").performClick()
        assertTrue(opened)
    }

    @Test fun `alphabet edge gesture reaches the final letter and search hides the scrubber`() {
        val apps = ('A'..'Z').flatMap { letter -> (0..2).map { AppEntry(AppTarget("test.app", "$letter$it"), "$letter app $it") } }
        var launched = false
        render {
            AppListScreen(apps, false, false, onBack = {}, onSelect = { launched = true }, onSettings = {}, alphabetScrubber = true)
        }
        rule.onNodeWithTag("alphabet-scrubber").performTouchInput { down(topCenter); moveTo(bottomCenter, 400) }
        rule.onNodeWithText("Z").assertIsDisplayed()
        screenshot("apps-alphabet-active")
        rule.onNodeWithTag("alphabet-scrubber").performTouchInput { up() }
        rule.onNodeWithText("Z").assertDoesNotExist()
        rule.onNodeWithText("Z app 2").assertIsDisplayed()
        assertFalse(launched)
        screenshot("apps-alphabet")
        rule.onNodeWithText("Search apps").performTextInput("M app")
        rule.onNodeWithTag("alphabet-scrubber").assertDoesNotExist()
        rule.onNodeWithText("M app 0").assertIsDisplayed()
        rule.onNodeWithText("Clear").performClick()
        rule.onNodeWithTag("alphabet-scrubber").assertIsDisplayed()
    }

    @Test fun `alphabet navigation supports accessibility progress actions`() {
        val apps = ('A'..'Z').map { AppEntry(AppTarget("test.app", "$it"), "$it app") }
        render { AppListScreen(apps, false, false, onBack = {}, onSelect = {}, alphabetScrubber = true) }
        rule.onNodeWithTag("alphabet-scrubber").performSemanticsAction(SemanticsActions.SetProgress) { it(25f) }
        rule.onNodeWithText("Z app").assertIsDisplayed()
    }

    @Test fun `alphabet navigation is optional and absent for empty results`() {
        val apps = listOf("Calendar", "Music").map { AppEntry(AppTarget("test.app", it), it) }
        render { AppListScreen(apps, false, false, onBack = {}, onSelect = {}) }
        rule.onNodeWithTag("alphabet-scrubber").assertDoesNotExist()
    }

    @Test fun `fractional accessibility adjustments move between two alphabet sections`() {
        var first by mutableIntStateOf(0)
        render { AlphabetScrubber(listOf(AlphabetSection("A", 0), AlphabetSection("Z", 10)), first, { first = it }) }
        val scrubber = rule.onNodeWithTag("alphabet-scrubber")
        scrubber.performSemanticsAction(SemanticsActions.SetProgress) { it(0.1f) }
        scrubber.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Z"))
        scrubber.performSemanticsAction(SemanticsActions.SetProgress) { it(0.9f) }
        scrubber.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "A"))
    }

    @Test fun `violet light palette home renders`() {
        render(LauncherPreferences(colorStyle = ColorStyle.COLOR, themeMode = ThemeMode.LIGHT)) {
            HomeScreen(HomeLayout.LIST, "09:41", "Friday, October 30", {}, {}, shortcuts = legacyHomeShortcuts())
        }
        screenshot("home-violet-light")
    }

    @Test fun `violet dark palette draws the generated background`() {
        val p = LauncherPreferences(colorStyle = ColorStyle.COLOR, themeMode = ThemeMode.DARK)
        render(p) { HomeScreen(HomeLayout.LIST, "09:41", "Friday, October 30", {}, {}, shortcuts = legacyHomeShortcuts()) }
        val bitmap = screenshot("home-violet-dark")
        assertEquals(Palette.generate(p, false).background, bitmap.getPixel(0, 0))
    }

    @Test fun `color settings expose system mode and paired previews`() {
        val p = LauncherPreferences(colorStyle = ColorStyle.COLOR, themeMode = ThemeMode.DARK)
        render(p) {
            detail(SettingsSection.APPEARANCE, p)
        }
        rule.onNodeWithText("System").assertIsDisplayed()
        screenshot("settings-violet-dark")
    }

    @Test fun `Home text size selection updates its preview and preference`() {
        var p by mutableStateOf(LauncherPreferences(layout = HomeLayout.LIST, pixelClockLayout = false))
        render {
            detail(SettingsSection.HOME, p, onChange = { change -> p = change(p) })
        }
        val before = rule.onNodeWithText("09:41").performScrollTo().fetchSemanticsNode().boundsInRoot.height
        rule.onNodeWithText("Large").performScrollTo().performClick()
        assertEquals(HomeTextSize.LARGE, p.homeTextSize)
        assertTrue(rule.onNodeWithText("09:41").performScrollTo().fetchSemanticsNode().boundsInRoot.height > before)
        screenshot("settings-home-size")
    }

    @Test @Config(qualifiers = "w344dp-h768dp-xhdpi")
    fun `Pixel header matches screenshot ink anchors and stays fixed with status bar insets`() {
        var inset by mutableStateOf(0.dp)
        var clockOpened = false
        var dateOpened = false
        render {
            Box(Modifier.fillMaxSize().padding(top = inset)) {
                HomeScreen(HomeLayout.GRID, "14:03", "Fri, 30 Oct", { clockOpened = true }, {},
                    contentTopInset = inset, onDate = { dateOpened = true }, shortcuts = legacyHomeShortcuts())
            }
        }
        fun ink(bitmap: android.graphics.Bitmap, area: android.graphics.Rect): android.graphics.Rect {
            val result = android.graphics.Rect(area.right, area.bottom, area.left, area.top)
            for (y in area.top until area.bottom) for (x in area.left until area.right) {
                val pixel = bitmap.getPixel(x, y)
                if (android.graphics.Color.red(pixel) > 120 && android.graphics.Color.green(pixel) > 120 && android.graphics.Color.blue(pixel) > 120) {
                    result.left = minOf(result.left, x); result.top = minOf(result.top, y)
                    result.right = maxOf(result.right, x + 1); result.bottom = maxOf(result.bottom, y + 1)
                }
            }
            return result
        }
        fun check(bitmap: android.graphics.Bitmap) {
            val clock = ink(bitmap, android.graphics.Rect(15, 130, 340, 300))
            val date = ink(bitmap, android.graphics.Rect(30, 320, 220, 410))
            assertEquals(46f, clock.left.toFloat(), 2f); assertEquals(186f, clock.top.toFloat(), 2f)
            assertEquals(272f, clock.width().toFloat(), 2f); assertEquals(89f, clock.height().toFloat(), 2f)
            assertEquals(54f, date.left.toFloat(), 2f); assertEquals(350f, date.top.toFloat(), 2f)
            assertEquals(126f, date.width().toFloat(), 2f); assertEquals(23f, date.height().toFloat(), 2f)
        }
        rule.onNodeWithText("14 03").assertIsDisplayed()
        check(screenshot("home-pixel-reference"))
        rule.onNodeWithTag("home-clock").performClick()
        rule.onNodeWithTag("home-date").performClick()
        assertTrue(clockOpened)
        assertTrue(dateOpened)
        rule.runOnIdle { inset = 24.dp }
        check(screenshot("home-pixel-reference-insets"))
    }

    @Test fun `Home settings can switch between the Pixel header and the scalable classic clock`() {
        var p by mutableStateOf(LauncherPreferences(layout = HomeLayout.LIST))
        render { detail(SettingsSection.HOME, p, onChange = { p = it(p) }) }
        rule.onNodeWithText("App text size").assertIsDisplayed()
        rule.onNodeWithTag("pixel-clock-header").assertExists()
        rule.onNodeWithText("Pixel-style clock").performClick()
        assertFalse(p.pixelClockLayout)
        rule.onNodeWithText("Home text size").assertIsDisplayed()
        rule.onNodeWithText("09:41").assertExists()
        rule.onNodeWithTag("pixel-clock-header").assertDoesNotExist()
    }

    @Test @Config(qualifiers = "w892dp-h412dp-xhdpi")
    fun `landscape uses the compact scalable clock instead of portrait lock screen anchors`() {
        render { HomeScreen(HomeLayout.GRID, "14:03", "Fri, 30 Oct", {}, {}, shortcuts = legacyHomeShortcuts()) }
        rule.onNodeWithTag("pixel-clock-header").assertDoesNotExist()
        rule.onNodeWithText("14:03").assertIsDisplayed()
        rule.onNodeWithContentDescription("Apps").assertDoesNotExist()
    }

    @Test fun `color wheel and brightness save the chosen seed and render both previews`() {
        val p = LauncherPreferences(colorStyle = ColorStyle.COLOR, themeMode = ThemeMode.LIGHT)
        var changed = p
        var saves = 0
        render(p) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                ColorSettings(p) { changed = it(changed); saves++ }
            }
        }
        rule.onNodeWithTag("color-preview-Light").assertIsDisplayed()
        rule.onNodeWithTag("color-preview-Dark").assertIsDisplayed()
        screenshot("color-picker")
        rule.onNodeWithTag("color-wheel").performTouchInput { down(centerRight) }
        rule.onNodeWithText("#ED0000").assertIsDisplayed()
        assertEquals(0, saves)
        rule.onNodeWithTag("color-wheel").performTouchInput { up() }
        assertEquals("#ED0000", changed.seedColor)
        assertEquals(1, saves)
        rule.onNodeWithContentDescription("Color brightness").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        assertEquals("#800000", changed.seedColor)
    }

    @Test fun `hex color previews accept valid input and prevent invalid saves`() {
        val p = LauncherPreferences(colorStyle = ColorStyle.COLOR)
        var changed = p
        render(p) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                ColorSettings(p) { changed = it(changed) }
            }
        }
        rule.onNodeWithText("Hex color").performTextReplacement("invalid")
        rule.onNodeWithText("Apply").assertIsNotEnabled()
        assertEquals(p, changed)
        rule.onNodeWithText("Hex color").performTextReplacement("#00aaff")
        rule.onNodeWithText("Apply").performClick()
        assertEquals("#00AAFF", changed.seedColor)
    }

    @Test fun `settings overview opens focused pages and always offers changing launcher`() {
        var section by mutableStateOf<SettingsSection?>(null)
        var changedLauncher = false
        render {
            val current = section
            if (current == null) SettingsScreen(true, false, {}, { section = it }, {}, { changedLauncher = true })
            else detail(current, onBack = { section = null })
        }
        SettingsSection.entries.forEach { rule.onNodeWithText(it.title).assertExists() }
        screenshot("settings-overview")
        rule.onNodeWithText("Appearance").performClick()
        rule.onNodeWithText("System").assertIsDisplayed()
        rule.onNodeWithText("Home shortcuts").assertDoesNotExist()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Home shortcuts").performClick()
        rule.onNodeWithText("Add shortcut").assertIsDisplayed()
        rule.onNodeWithText("System").assertDoesNotExist()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Default launcher").performScrollTo().performClick()
        assertTrue(changedLauncher)
    }

    @Test fun `About shows the app version privacy and working project links`() {
        var opened = ""
        render { detail(SettingsSection.ABOUT, onOpenLink = { opened = it }) }
        rule.onNodeWithText("Version ${BuildConfig.VERSION_NAME}").assertIsDisplayed()
        rule.onNodeWithText("No accounts, ads or analytics.").assertIsDisplayed()
        screenshot("about-nia")
        rule.onNodeWithText("View on GitHub").performScrollTo().performClick()
        assertEquals(ProjectLinks.REPOSITORY, opened)
        rule.onNodeWithText("Privacy policy").performScrollTo().performClick()
        assertEquals(ProjectLinks.PRIVACY, opened)
        rule.onNodeWithText("Third-party notices").performScrollTo().performClick()
        assertEquals(ProjectLinks.NOTICES, opened)
    }

    @Test fun `backup page keeps full reset behind explicit confirmation`() {
        var resets = 0
        render { detail(SettingsSection.BACKUP, onResetAll = { resets++ }) }
        rule.onNodeWithText("Reset all settings").performClick()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(0, resets)
        rule.onNodeWithText("Reset all settings").performClick()
        rule.onNode(hasText("Reset") and hasClickAction()).performClick()
        assertEquals(1, resets)
    }

    @Test fun `guided setup can add a custom app and keeps its step through the editor and picker`() {
        var saved by mutableStateOf(SavedState(preferences = LauncherPreferences(themeMode = ThemeMode.DARK)))
        var draft by mutableStateOf<HomeShortcutDraft?>(null)
        var picker by mutableStateOf(false)
        var isDefault by mutableStateOf(false)
        val signal = AppEntry(AppTarget("org.example.signal", "Main"), "Signal")
        render {
            NiaTheme(Palette.generate(saved.preferences, false)) {
                Surface(Modifier.fillMaxSize()) {
                    val current = draft
                    when {
                        saved.setupComplete -> HomeScreen(saved.preferences.layout, "09:41", "Friday, October 30", {}, {},
                            shortcuts = saved.preferences.homeShortcuts)
                        picker -> AppListScreen(listOf(signal), false, false, onBack = { picker = false }, onSelect = { app ->
                            draft = current!!.copy(shortcut = current.shortcut.copy(app = app.target, name = app.label)); picker = false
                        })
                        current != null -> HomeShortcutEditorScreen(current, listOf(signal), false,
                            onChange = { draft = current.copy(shortcut = it(current.shortcut)) }, onChooseApp = { picker = true },
                            onSave = { saved = saved.copy(preferences = saved.preferences.copy(homeShortcuts = listOf(current.shortcut))); draft = null },
                            onRemove = {}, onBack = { draft = null })
                        else -> OnboardingScreen(saved.preferences, listOf(signal), saved.setupStep, isDefault,
                            onChange = { change -> saved = saved.copy(preferences = change(saved.preferences)) },
                            onStep = { saved = saved.copy(setupStep = it) },
                            onEditHome = { draft = HomeShortcutDraft(HomeShortcut("signal", ""), true) }, onMoveHome = { _, _ -> },
                            onDefault = { isDefault = true }, onStart = { saved = saved.copy(setupComplete = true) }, onApps = {}, onImport = {})
                    }
                }
            }
        }
        rule.onNodeWithText("1 of 4").assertIsDisplayed()
        screenshot("onboarding-layout")
        rule.onNodeWithText("Text list").performClick()
        rule.onNodeWithText("Continue").performClick()
        rule.onNodeWithText("Skip for now").assertIsDisplayed()
        rule.onNodeWithText("Add shortcut").performScrollTo().performClick()
        rule.onNodeWithText("Save").assertIsNotEnabled()
        rule.onNodeWithText("Choose an app").performClick()
        rule.onNodeWithText("Signal").performClick()
        rule.onNodeWithTag("home-shortcut-name").performTextReplacement("Messages")
        rule.onNodeWithText("Save").performScrollTo().performClick()
        rule.onNodeWithText("2 of 4").assertIsDisplayed()
        rule.onNodeWithText("Messages").assertIsDisplayed()
        assertEquals(signal.target, saved.preferences.homeShortcuts.single().app)
        rule.onNodeWithText("Continue").performClick()
        rule.onNodeWithText("3 of 4").assertIsDisplayed()
        rule.onNodeWithText("Color").performClick()
        rule.onNodeWithTag("color-wheel").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Continue").performClick()
        rule.onNodeWithText("4 of 4").assertIsDisplayed()
        rule.onNodeWithText("Set as default launcher").performClick()
        rule.onNodeWithText("nia is your default launcher.").assertIsDisplayed()
        screenshot("onboarding-ready")
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithText("3 of 4").assertIsDisplayed()
        rule.onNodeWithText("Continue").performClick()
        rule.onNodeWithText("Start using nia").performClick()
        assertTrue(saved.setupComplete)
        rule.onNodeWithText("Messages").assertIsDisplayed()
        rule.onNodeWithText("Apps").assertDoesNotExist()
    }

    @Test fun `Add your apps can skip to appearance without populating Home`() {
        var step by mutableStateOf(SetupStep.APPS)
        val p = LauncherPreferences()
        render {
            OnboardingScreen(p, emptyList(), step, false, {}, { step = it }, {}, { _, _ -> }, {}, {}, {}, {})
        }
        rule.onNodeWithText("Skip for now").performClick()
        assertEquals(SetupStep.APPEARANCE, step)
        assertTrue(p.homeShortcuts.isEmpty())
    }

    @Test fun `setup recovery keeps app access and restore available without overwriting bad settings`() {
        var restored = false
        var recovery = false
        var apps = false
        render {
            OnboardingScreen(LauncherPreferences(), emptyList(), SetupStep.LAYOUT, false, {}, {}, {}, { _, _ -> }, {}, {},
                { apps = true }, { restored = true }, storageError = true, onRecovery = { recovery = true })
        }
        rule.onNodeWithText("Continue").assertIsNotEnabled()
        rule.onNodeWithText("Skip setup").assertDoesNotExist()
        rule.onNodeWithText("All apps").performClick()
        rule.onNodeWithText("Restore a settings backup").performClick()
        rule.onNodeWithText("Open recovery settings").performClick()
        assertTrue(apps && restored && recovery)
    }

    @Test @Config(qualifiers = "w320dp-h600dp-xhdpi")
    fun `setup navigation remains reachable with large system text on a compact screen`() {
        var step by mutableStateOf(SetupStep.APPEARANCE)
        val p = LauncherPreferences(colorStyle = ColorStyle.COLOR, themeMode = ThemeMode.DARK)
        render(p) {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                OnboardingScreen(p, emptyList(), step, false, {}, { step = it }, {}, { _, _ -> }, {}, {}, {}, {})
            }
        }
        rule.onNodeWithText("Continue").assertIsDisplayed()
        rule.onNodeWithText("All apps").assertIsDisplayed()
        rule.onNodeWithTag("color-wheel").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Continue").performClick()
        rule.onNodeWithText("Start using nia").assertIsDisplayed()
        rule.onNodeWithText("Set as default launcher").performScrollTo().assertIsDisplayed()
        screenshot("onboarding-large-text")
    }
}
