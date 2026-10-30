package dev.robinsiegl.nia

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.robinsiegl.nia.data.BackupCodec
import dev.robinsiegl.nia.data.BackupException
import dev.robinsiegl.nia.data.PreferencesStore
import dev.robinsiegl.nia.model.*
import dev.robinsiegl.nia.platform.AppRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class RestorePreview(val preferences: LauncherPreferences, val missingApps: Int)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val store = PreferencesStore(application)
    private val repository = AppRepository(application)
    private val _saved = MutableStateFlow<SavedState?>(null)
    val saved = _saved.asStateFlow()
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps = _apps.asStateFlow()
    private val _loadingApps = MutableStateFlow(true)
    val loadingApps = _loadingApps.asStateFlow()
    private val _navigation = MutableStateFlow(Navigation())
    val navigation = _navigation.asStateFlow()
    private val _isDefault = MutableStateFlow(false)
    val isDefault = _isDefault.asStateFlow()
    private val _storageError = MutableStateFlow(false)
    val storageError = _storageError.asStateFlow()
    private val _restore = MutableStateFlow<RestorePreview?>(null)
    val restore = _restore.asStateFlow()
    private val events = Channel<String>(Channel.BUFFERED)
    val messages = events.receiveAsFlow()
    private var settingsObserver: Job? = null

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
    fun back() { _navigation.update { it.back() } }
    fun home() { _navigation.value = Navigation() }
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

    fun chooseApp(category: HomeCategory?, target: AppTarget) = perform("The app assignment could not be saved.") {
        store.update { state ->
            val p = state.preferences
            state.copy(preferences = if (category == null) p.copy(clock = target)
            else p.copy(assignments = p.assignments + (category to target)))
        }
        back()
    }

    fun clearAssignment(category: HomeCategory?) = updatePreferences {
        if (category == null) it.copy(clock = null) else it.copy(assignments = it.assignments - category)
    }

    fun openCategory(category: HomeCategory) {
        val target = _saved.value?.preferences?.assignments?.get(category)
        if (target == null) { navigate(Screen.Picker(category)); return }
        try { repository.launch(target); home() }
        catch (_: RuntimeException) {
            navigate(Screen.Picker(category))
            events.trySend("That app is unavailable. Choose another app for ${category.label}.")
        }
    }

    fun openClock() {
        try {
            val clock = _saved.value?.preferences?.clock
            if (clock == null) repository.openClock() else repository.launch(clock)
            home()
        } catch (_: RuntimeException) {
            navigate(Screen.Picker(null))
            events.trySend("Choose the clock app you want to open.")
        }
    }

    fun openApp(target: AppTarget) {
        try { repository.launch(target); home() }
        catch (_: RuntimeException) { events.trySend("This app is currently unavailable.") }
    }

    fun showAppInfo(target: AppTarget) {
        try { repository.showAppInfo(target) }
        catch (_: RuntimeException) { events.trySend("App info is currently unavailable.") }
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
        val available = withContext(Dispatchers.IO) { repository.listApps().map { it.target }.toSet() }
        val missing = decoded.assignments.values.count { it !in available } +
            if (decoded.clock != null && decoded.clock !in available) 1 else 0
        _restore.value = RestorePreview(decoded.copy(
            assignments = decoded.assignments.filterValues { it in available },
            clock = decoded.clock?.takeIf { it in available },
        ), missing)
    }

    fun cancelRestore() { _restore.value = null }
    fun applyRestore() = perform("Your settings could not be restored. Your previous settings are unchanged.") {
        val preview = _restore.value ?: return@perform
        // A restore is an explicit recovery path even if the existing preferences are corrupt.
        val restored = SavedState(preferences = preview.preferences, setupComplete = true)
        store.replace(restored)
        _saved.value = restored
        observeSettings()
        _storageError.value = false; _restore.value = null
        home(); events.send("Settings restored.")
    }

    fun resetAppearance() = updatePreferences { current ->
        LauncherPreferences(assignments = current.assignments, clock = current.clock)
    }

    fun resetAll() = perform("nia could not be reset.") {
        store.reset(); _storageError.value = false; _saved.value = SavedState(); observeSettings(); home()
    }
}
