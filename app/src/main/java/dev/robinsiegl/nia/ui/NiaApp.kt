package dev.robinsiegl.nia.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.robinsiegl.nia.LauncherViewModel
import dev.robinsiegl.nia.model.*
import dev.robinsiegl.nia.theme.NiaPalette
import dev.robinsiegl.nia.theme.Palette
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@Composable
fun NiaTheme(palette: NiaPalette, content: @Composable () -> Unit) {
    val bg = Color(palette.background); val fg = Color(palette.foreground)
    val base = if (palette.isDark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = fg, onPrimary = bg, primaryContainer = bg, onPrimaryContainer = fg,
        secondary = fg, onSecondary = bg, secondaryContainer = bg, onSecondaryContainer = fg,
        tertiary = fg, onTertiary = bg, background = bg, onBackground = fg,
        error = fg, onError = bg, errorContainer = bg, onErrorContainer = fg,
        surface = bg, onSurface = fg, surfaceVariant = bg, onSurfaceVariant = fg,
        surfaceTint = Color.Transparent, inverseSurface = fg, inverseOnSurface = bg,
        outline = fg.copy(alpha = 0.4f), outlineVariant = fg.copy(alpha = 0.2f),
        surfaceContainer = bg, surfaceContainerHigh = bg, surfaceContainerHighest = bg,
        surfaceContainerLow = bg, surfaceContainerLowest = bg,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun NiaApp(model: LauncherViewModel, homeRequest: Int, unlockRequest: UnlockRevealRequest? = null, deviceLocked: Boolean = false,
    revealAllowed: Boolean = true, onSystemBars: (Boolean) -> Unit) {
    val saved by model.saved.collectAsStateWithLifecycle()
    val apps by model.apps.collectAsStateWithLifecycle()
    val loading by model.loadingApps.collectAsStateWithLifecycle()
    val selectingApp by model.selectingApp.collectAsStateWithLifecycle()
    val navigation by model.navigation.collectAsStateWithLifecycle()
    val homeDraft by model.homeDraft.collectAsStateWithLifecycle()
    val searchAppsOnOpen by model.searchAppsOnOpen.collectAsStateWithLifecycle()
    val isDefault by model.isDefault.collectAsStateWithLifecycle()
    val storageError by model.storageError.collectAsStateWithLifecycle()
    val restore by model.restore.collectAsStateWithLifecycle()
    val restoring by model.restoring.collectAsStateWithLifecycle()
    val updateCheck by model.updateCheck.collectAsStateWithLifecycle()
    val preferences = saved?.preferences ?: LauncherPreferences()
    val palette = Palette.generate(preferences, isSystemInDarkTheme())
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    SideEffect { onSystemBars(palette.isDark) }
    LaunchedEffect(homeRequest) {
        if (homeRequest > 0) { focus.clearFocus(); keyboard?.hide(); model.home() }
    }
    LaunchedEffect(saved?.setupComplete, navigation.screen, unlockRequest) {
        if (saved?.setupComplete == false || navigation.screen != Screen.Home) unlockRequest?.consume()
    }
    LaunchedEffect(model) { model.messages.collect { snackbar.showSnackbar(it) } }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        uri -> if (uri != null) model.exportSettings(uri)
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        uri -> if (uri != null) model.previewImport(uri)
    }
    val role = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { model.refreshDefault() }
    val changeDefault: () -> Unit = {
        try { role.launch(Intent(Settings.ACTION_HOME_SETTINGS)) }
        catch (_: RuntimeException) { scope.launch { snackbar.showSnackbar("Open Android Settings → Apps → Default apps → Home app to change your launcher.") } }
    }
    val setDefault: () -> Unit = {
        try { role.launch(model.homeSelectionIntent()) }
        catch (_: RuntimeException) {
            try { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
            catch (_: RuntimeException) { scope.launch { snackbar.showSnackbar("Open Android Settings → Apps → Default apps → Home app and choose nia.") } }
        }
    }
    val beginImport: () -> Unit = {
        import.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
    }
    val openLink: (String) -> Unit = { url ->
        try { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
        catch (_: RuntimeException) { scope.launch { snackbar.showSnackbar("No browser is available to open this link.") } }
    }
    NiaTheme(palette) {
        Scaffold(containerColor = Color(palette.background), contentColor = Color(palette.foreground),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                if (saved == null) {
                    CircularProgressIndicator(modifier = Modifier.padding(28.dp))
                } else {
                    val screen = navigation.screen
                    BackHandler(enabled = screen == Screen.Home) { /* Keep the Home surface available. */ }
                    BackHandler(enabled = screen == Screen.Settings || screen is Screen.SettingsDetail) { model.back() }
                    if (saved?.setupComplete == true && (screen == Screen.Home || screen == Screen.Apps)) {
                        val (time, date) = rememberClockText()
                        HomeAppsScreen(preferences, apps, loading, time, date, drawerOpen = screen == Screen.Apps,
                            onDrawerOpenChanged = { if (it) model.openApps() else model.back() },
                            onClock = model::openClock, onShortcut = model::openHomeShortcut, onDate = model::openCalendar,
                            onSettings = { model.navigate(Screen.Settings) }, onApp = { model.openApp(it.target) },
                            onAppInfo = { model.showAppInfo(it.target) }, onCorner = model::openCorner,
                            contentTopInset = padding.calculateTopPadding(), unlockRequest = unlockRequest,
                            deviceLocked = deviceLocked, homeRequest = homeRequest,
                            onAssignCorner = { app, position -> model.assignCornerFromApps(position, app) },
                            revealAllowed = revealAllowed, searchAppsOnOpen = searchAppsOnOpen,
                            onAddHome = model::addHomeFromApps, onReorderHome = model::reorderHomeShortcuts,
                            onEditHome = model::editHomeShortcut)
                    } else key(screen) {
                        when (screen) {
                            Screen.Home -> {
                                OnboardingScreen(preferences, apps, saved?.setupStep ?: SetupStep.LAYOUT, isDefault,
                                    onChange = model::updatePreferences, onStep = model::moveSetup,
                                    onEditHome = model::editHomeShortcut, onMoveHome = model::moveHomeShortcut,
                                    onDefault = setDefault, onStart = model::finishSetup,
                                    onApps = { model.navigate(Screen.Apps) }, onImport = beginImport,
                                    storageError = storageError, onReorderHome = model::reorderHomeShortcuts,
                                    onRecovery = { model.navigate(Screen.SettingsDetail(SettingsSection.BACKUP)) })
                            }
                            Screen.Apps -> AppListScreen(apps, loading, preferences.searchAutofocus,
                                onBack = model::back, onSelect = { model.openApp(it.target) },
                                onSettings = { model.navigate(Screen.Settings) }, onInfo = { model.showAppInfo(it.target) },
                                alphabetScrubber = preferences.alphabetScrubber,
                                onAddHome = model::addHomeFromApps, canAddHome = preferences.homeShortcuts.size < MAX_HOME_SHORTCUTS,
                                onAssignCorner = { app, position -> model.assignCornerFromApps(position, app) })
                            is Screen.Picker -> AppListScreen(apps, loading, false,
                                title = screen.header.label, onBack = model::back,
                                onSelect = { model.chooseApp(screen.header, it.target) }, alphabetScrubber = preferences.alphabetScrubber,
                                interactionEnabled = !selectingApp)
                            Screen.HomeShortcutEditor -> homeDraft?.let { draft ->
                                HomeShortcutEditorScreen(draft, apps, selectingApp, model::updateHomeDraft,
                                    onChooseApp = { model.navigate(Screen.HomeShortcutPicker) }, onSave = model::saveHomeShortcut,
                                    onRemove = { model.removeHomeShortcut(draft.shortcut.id) }, onBack = model::back)
                            }
                            Screen.HomeShortcutPicker -> AppListScreen(apps, loading, false,
                                title = "Choose app", onBack = model::back, onSelect = model::chooseHomeApp,
                                alphabetScrubber = preferences.alphabetScrubber)
                            is Screen.CornerPicker -> AppListScreen(apps, loading, false,
                                title = screen.position.label + " quick corner", onBack = model::back,
                                onSelect = { model.chooseCornerApp(screen.position, it.target) }, alphabetScrubber = preferences.alphabetScrubber,
                                interactionEnabled = !selectingApp)
                            Screen.Settings -> SettingsScreen(isDefault, storageError, onBack = model::back,
                                onSection = { model.navigate(Screen.SettingsDetail(it)) },
                                onSetDefault = setDefault, onChangeDefault = changeDefault, preferences = preferences)
                            is Screen.SettingsDetail -> SettingsDetailScreen(screen.section, preferences, apps, storageError,
                                onBack = model::back, onChange = model::updatePreferences,
                                onAssign = { model.navigate(Screen.Picker(it)) }, onClear = model::clearAssignment,
                                onExport = { export.launch("nia-settings-${LocalDate.now()}.json") }, onImport = beginImport,
                                onResetAppearance = model::resetAppearance, onResetAll = model::resetAll, onOpenLink = openLink,
                                onAssignCorner = { model.navigate(Screen.CornerPicker(it)) },
                                onEditHome = model::editHomeShortcut, onMoveHome = model::moveHomeShortcut,
                                updateCheck = updateCheck, onCheckUpdates = model::checkForUpdates, onReorderHome = model::reorderHomeShortcuts)
                        }
                    }
                }
            }
        }
        restore?.let { preview ->
            RestoreDialog(preview, restoring, model::applyRestore, model::cancelRestore)
        }
    }
}

@Composable
private fun rememberClockText(): Pair<String, String> {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var now by remember { mutableStateOf(Date()) }
    DisposableEffect(context, owner) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { now = Date() }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK); addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED); addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) now = Date() }
        owner.lifecycle.addObserver(observer)
        onDispose { context.unregisterReceiver(receiver); owner.lifecycle.removeObserver(observer) }
    }
    return DateFormat.getTimeFormat(context).format(now) to
        SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEE MMM d"), Locale.getDefault()).format(now)
}
