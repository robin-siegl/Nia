package dev.robinsiegl.nia

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.robinsiegl.nia.data.BackupCodec
import dev.robinsiegl.nia.data.BackupException
import dev.robinsiegl.nia.data.PreferencesStore
import dev.robinsiegl.nia.model.*
import dev.robinsiegl.nia.platform.AppRepository
import dev.robinsiegl.nia.platform.ReleaseRepository
import dev.robinsiegl.nia.platform.UpdateCheckException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    internal val unlockSession = UnlockSession()
    private val store = PreferencesStore(application)
    private val repository = AppRepository(application)
    private val releases = ReleaseRepository()
    private val _saved = MutableStateFlow<SavedState?>(null)
    val saved = _saved.asStateFlow()
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps = _apps.asStateFlow()
    private val _loadingApps = MutableStateFlow(true)
    val loadingApps = _loadingApps.asStateFlow()
    private val _selectingApp = MutableStateFlow(false)
    val selectingApp = _selectingApp.asStateFlow()
    private val _homeDraft = MutableStateFlow<HomeShortcutDraft?>(null)
    val homeDraft = _homeDraft.asStateFlow()
    private val _navigation = MutableStateFlow(Navigation())
    val navigation = _navigation.asStateFlow()
    private val _isDefault = MutableStateFlow(false)
    val isDefault = _isDefault.asStateFlow()
    private val _storageError = MutableStateFlow(false)
    val storageError = _storageError.asStateFlow()
    private val _restore = MutableStateFlow<RestorePreview?>(null)
    val restore = _restore.asStateFlow()
    private val _restoring = MutableStateFlow(false)
    val restoring = _restoring.asStateFlow()
    private val _updateCheck = MutableStateFlow<UpdateCheck>(UpdateCheck.Idle)
    val updateCheck = _updateCheck.asStateFlow()
    private val events = Channel<String>(Channel.BUFFERED)
    val messages = events.receiveAsFlow()
    private var settingsObserver: Job? = null
    private val launchGate = LaunchGate()

    init {
        observeSettings()
        viewModelScope.launch {
            repository.apps.flowOn(Dispatchers.IO).catch {
                _loadingApps.value = false
                events.send("Apps could not be loaded. Reopen nia to try again.")
            }.collect { _apps.value = it; _loadingApps.value = false }
        }
    }

    private fun observeSettings() {
        settingsObserver?.cancel()
        settingsObserver = viewModelScope.launch {
            store.state.catch {
                _storageError.value = true
                events.send("Saved settings could not be read. You can restore a backup or reset nia.")
                emit(SavedState())
            }.collect { _saved.value = it }
        }
    }

    fun navigate(screen: Screen) { _navigation.update { it.push(screen) } }
    fun back() {
        if (_navigation.value.screen == Screen.HomeShortcutEditor) _homeDraft.value = null
        _navigation.update { it.back() }
    }
    fun home() { _homeDraft.value = null; _navigation.value = Navigation() }
    fun refreshDefault() { _isDefault.value = repository.isDefaultHome() }
    fun homeSelectionIntent() = repository.homeSelectionIntent()

    private fun perform(error: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: BackupException) { events.send(e.message ?: error) }
            catch (_: Exception) { events.send(error) }
        }
    }

    fun updatePreferences(transform: (LauncherPreferences) -> LauncherPreferences) =
        perform("Your settings could not be saved.") { store.update { it.copy(preferences = transform(it.preferences)) } }

    fun moveSetup(step: SetupStep) = perform("Your setup progress could not be saved.") {
        store.update { it.copy(setupStep = step) }
    }

    fun finishSetup() = perform("Setup could not be saved.") {
        store.update { it.copy(setupComplete = true) }; home()
    }

    fun chooseApp(header: HeaderTarget, target: AppTarget) = selectApp { p ->
        if (header == HeaderTarget.CLOCK) p.copy(clock = target) else p.copy(calendar = target)
    }

    fun editHomeShortcut(id: String? = null) {
        val preferences = _saved.value?.preferences ?: return
        val existing = preferences.homeShortcuts.find { it.id == id }
        if (id != null && existing == null) return
        if (existing == null && preferences.homeShortcuts.size >= MAX_HOME_APPS) return
        _homeDraft.value = HomeShortcutDraft(existing ?: HomeShortcut(UUID.randomUUID().toString(), ""), existing == null)
        navigate(Screen.HomeShortcutEditor)
    }

    fun updateHomeDraft(transform: (HomeShortcut) -> HomeShortcut) {
        _homeDraft.update { it?.copy(shortcut = transform(it.shortcut)) }
    }

    fun chooseHomeApp(app: AppEntry) {
        if (_navigation.value.screen != Screen.HomeShortcutPicker || _homeDraft.value == null) return
        updateHomeDraft { it.copy(app = app.target, name = it.name.ifBlank { app.label.take(MAX_HOME_NAME_LENGTH).trim().filterNot(Char::isISOControl) }) }
        back()
    }

    fun saveHomeShortcut() {
        val draft = _homeDraft.value ?: return
        if (!draft.canSave || !_selectingApp.compareAndSet(false, true)) return
        perform("The Home shortcut could not be saved.") {
            try {
                val shortcut = draft.shortcut.copy(name = draft.shortcut.name.trim())
                store.update { state ->
                    val entries = state.preferences.homeShortcuts
                    val next = if (draft.isNew) {
                        require(entries.size < MAX_HOME_APPS)
                        entries + shortcut
                    } else {
                        require(entries.any { it.id == shortcut.id })
                        entries.map { if (it.id == shortcut.id) shortcut else it }
                    }
                    state.copy(preferences = state.preferences.copy(homeShortcuts = next))
                }
                back()
            } finally { _selectingApp.value = false }
        }
    }

    fun removeHomeShortcut(id: String) = perform("The Home shortcut could not be removed.") {
        store.update { it.copy(preferences = it.preferences.copy(homeShortcuts = it.preferences.homeShortcuts.filterNot { entry -> entry.id == id })) }
        if (_navigation.value.screen == Screen.HomeShortcutEditor) back()
    }

    fun moveHomeShortcut(id: String, direction: Int) = updatePreferences { p ->
        val entries = p.homeShortcuts.toMutableList()
        val index = entries.indexOfFirst { it.id == id }
        val destination = index + direction.coerceIn(-1, 1)
        if (index >= 0 && destination in entries.indices) entries.add(destination, entries.removeAt(index))
        p.copy(homeShortcuts = entries)
    }

    private fun selectApp(transform: (LauncherPreferences) -> LauncherPreferences) {
        if (!_selectingApp.compareAndSet(false, true)) return
        perform("The app assignment could not be saved.") {
            try {
                store.update { it.copy(preferences = transform(it.preferences)) }
                back()
            } finally { _selectingApp.value = false }
        }
    }

    fun clearAssignment(header: HeaderTarget) = updatePreferences {
        if (header == HeaderTarget.CLOCK) it.copy(clock = null) else it.copy(calendar = null)
    }

    fun chooseCornerApp(position: CornerPosition, target: AppTarget) = selectApp { p ->
        p.withShortcut(position, p.shortcut(position).copy(action = ShortcutAction.APP, app = target))
    }

    fun assignCornerFromApps(position: CornerPosition, app: AppEntry) = perform("The shortcut could not be saved.") {
        store.update { state -> state.copy(preferences = state.preferences.withShortcut(position,
            state.preferences.shortcut(position).copy(action = ShortcutAction.APP, app = app.target))) }
        events.send("${app.label} assigned to the ${position.label.lowercase()} corner.")
    }

    fun openCorner(position: CornerPosition) {
        val shortcut = _saved.value?.preferences?.shortcut(position) ?: return
        when (shortcut.action) {
            ShortcutAction.EMPTY -> Unit
            ShortcutAction.APP -> launch {
                try { repository.launch(requireNotNull(shortcut.app)); home() }
                catch (_: RuntimeException) {
                    launchGate.reset()
                    navigate(Screen.CornerPicker(position))
                    events.trySend("That shortcut app is unavailable. Choose another app.")
                }
            }
        }
    }

    private fun launch(action: () -> Unit) {
        if (launchGate.accept(SystemClock.elapsedRealtime())) action()
    }

    fun openHomeShortcut(id: String) {
        val shortcut = _saved.value?.preferences?.homeShortcuts?.find { it.id == id } ?: return
        val target = shortcut.app
        if (target == null) { editHomeShortcut(id); return }
        launch { try { repository.launch(target); home() }
        catch (_: RuntimeException) {
            launchGate.reset()
            editHomeShortcut(id)
            events.trySend("That app is unavailable. Choose another app for ${shortcut.name}.")
        } }
    }

    fun openCalendar() {
        val target = _saved.value?.preferences?.calendar
        if (target == null) { navigate(Screen.Picker(HeaderTarget.CALENDAR)); return }
        launch { try { repository.launch(target); home() }
        catch (_: RuntimeException) {
            launchGate.reset()
            navigate(Screen.Picker(HeaderTarget.CALENDAR))
            events.trySend("Choose the calendar app you want to open.")
        } }
    }

    fun openClock() {
        launch { try {
            val clock = _saved.value?.preferences?.clock
            if (clock == null) repository.openClock() else repository.launch(clock)
            home()
        } catch (_: RuntimeException) {
            launchGate.reset()
            navigate(Screen.Picker(HeaderTarget.CLOCK))
            events.trySend("Choose the clock app you want to open.")
        } }
    }

    fun openApp(target: AppTarget) {
        launch {
            try { repository.launch(target); home() }
            catch (_: RuntimeException) { launchGate.reset(); events.trySend("This app is currently unavailable.") }
        }
    }

    fun showAppInfo(target: AppTarget) {
        try { repository.showAppInfo(target) }
        catch (_: RuntimeException) { events.trySend("App info is currently unavailable.") }
    }

    fun checkForUpdates() {
        val previous = _updateCheck.value
        if (previous == UpdateCheck.Checking || !_updateCheck.compareAndSet(previous, UpdateCheck.Checking)) return
        viewModelScope.launch {
            _updateCheck.value = try { releases.latest()?.let { UpdateCheck.Result(it) } ?: UpdateCheck.NoRelease }
            catch (e: CancellationException) { _updateCheck.value = previous; throw e }
            catch (e: UpdateCheckException) { UpdateCheck.Failed(requireNotNull(e.message)) }
            catch (_: Exception) { UpdateCheck.Failed("Couldn’t check for updates. Check your connection and try again.") }
        }
    }

    fun exportSettings(uri: Uri) = perform("The settings file could not be saved.") {
        val p = _saved.value?.preferences ?: return@perform
        withContext(Dispatchers.IO) {
            val bytes = BackupCodec.export(p)
            val stream = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                ?: error("The file could not be opened")
            stream.use { it.write(bytes) }
        }
        events.send("Settings exported.")
    }

    fun previewImport(uri: Uri) = perform("The settings file could not be opened.") {
        val decoded = withContext(Dispatchers.IO) {
            val input = getApplication<Application>().contentResolver.openInputStream(uri)
                ?: error("The file could not be opened")
            input.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > BackupCodec.MAX_BYTES) throw BackupException("This settings file is too large.")
                    output.write(buffer, 0, count)
                }
                BackupCodec.decode(output.toByteArray())
            }
        }
        // Re-query before sanitizing: a just-installed app may not yet be in the UI catalog.
        val available = withContext(Dispatchers.IO) { repository.listApps() }
        _restore.value = RestorePreview.create(_saved.value?.preferences ?: LauncherPreferences(), decoded, available)
    }

    fun cancelRestore() { if (!_restoring.value) _restore.value = null }
    fun applyRestore() {
        val preview = _restore.value ?: return
        if (!_restoring.compareAndSet(false, true)) return
        perform("Your settings could not be restored. Your previous settings are unchanged.") {
            try {
                val available = withContext(Dispatchers.IO) { repository.listApps() }
                val fresh = RestorePreview.create(_saved.value?.preferences ?: LauncherPreferences(), preview.requested, available)
                if (fresh.preferences != preview.preferences || fresh.current != preview.current) {
                    _restore.value = fresh
                    events.send("Settings or available apps changed. Review the updated preview before restoring.")
                    return@perform
                }
                // An explicit restore can also recover corrupt preferences.
                val restored = SavedState(preferences = preview.preferences, setupComplete = true)
                store.replace(restored)
                _saved.value = restored
                observeSettings()
                _storageError.value = false; _restore.value = null
                home(); events.send("Settings restored.")
            } finally { _restoring.value = false }
        }
    }

    fun resetAppearance() = updatePreferences { current ->
        LauncherPreferences(homeShortcuts = current.homeShortcuts, clock = current.clock, calendar = current.calendar,
            leftShortcut = current.leftShortcut, rightShortcut = current.rightShortcut)
    }

    fun resetAll() = perform("nia could not be reset.") {
        store.reset(); _storageError.value = false; _saved.value = SavedState(); observeSettings(); home()
    }
}
