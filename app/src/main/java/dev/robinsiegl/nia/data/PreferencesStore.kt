package dev.robinsiegl.nia.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.robinsiegl.nia.model.PreferencesValidation
import dev.robinsiegl.nia.model.SavedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString

private val Context.niaDataStore by preferencesDataStore(name = "nia")

class PreferencesStore(context: Context) {
    private val store = context.applicationContext.niaDataStore
    private val key = stringPreferencesKey("state.v1")
    val state: Flow<SavedState> = store.data.map { data ->
        val state = data[key]?.let(BackupCodec::decodeState) ?: SavedState()
        PreferencesValidation.validate(state.preferences)
        state
    }

    suspend fun update(transform: (SavedState) -> SavedState) {
        store.edit { data ->
            val current = data[key]?.let(BackupCodec::decodeState) ?: SavedState()
            val next = transform(current)
            PreferencesValidation.validate(next.preferences)
            data[key] = BackupCodec.json.encodeToString(next)
        }
    }

    suspend fun reset() { store.edit { it.remove(key) } }

    suspend fun replace(state: SavedState) {
        PreferencesValidation.validate(state.preferences)
        store.edit { it[key] = BackupCodec.json.encodeToString(state) }
    }
}
