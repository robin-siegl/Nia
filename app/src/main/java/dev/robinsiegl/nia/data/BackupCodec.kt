package dev.robinsiegl.nia.data

import dev.robinsiegl.nia.model.LauncherPreferences
import dev.robinsiegl.nia.model.PreferencesValidation
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

@Serializable
private data class SettingsBackup(
    val format: String,
    val schemaVersion: Int,
    val profile: String,
    val exportedAt: String,
    val settings: LauncherPreferences,
)

class BackupException(message: String) : IllegalArgumentException(message)

object BackupCodec {
    const val MAX_BYTES = 256 * 1024
    private const val CURRENT_VERSION = 2
    val json = Json { encodeDefaults = true; prettyPrint = true; explicitNulls = false }

    fun export(preferences: LauncherPreferences, exportedAt: String = Instant.now().toString()): ByteArray {
        PreferencesValidation.validate(preferences)
        return json.encodeToString(SettingsBackup("nia.settings", CURRENT_VERSION, "personal", exportedAt, preferences))
            .toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): LauncherPreferences {
        if (bytes.size > MAX_BYTES) throw BackupException("This settings file is too large.")
        try {
            val text = bytes.toString(Charsets.UTF_8)
            val root = json.parseToJsonElement(text).jsonObject
            if (root["format"]?.jsonPrimitive?.content != "nia.settings") {
                throw BackupException("Choose a nia settings backup.")
            }
            val version = root["schemaVersion"]?.jsonPrimitive?.intOrNull
            if (version != 1 && version != CURRENT_VERSION) {
                throw BackupException("This backup version is not supported. Update nia and try again.")
            }
            if (root["profile"]?.jsonPrimitive?.content != "personal") {
                throw BackupException("This version of nia supports personal-profile backups only.")
            }
            val backup = json.decodeFromString<SettingsBackup>(text)
            PreferencesValidation.validate(backup.settings)
            return backup.settings
        } catch (e: BackupException) {
            throw e
        } catch (_: IllegalArgumentException) {
            throw BackupException("This settings file is invalid. Your current settings have not changed.")
        }
    }
}
