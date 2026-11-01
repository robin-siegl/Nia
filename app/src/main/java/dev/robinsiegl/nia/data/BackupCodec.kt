package dev.robinsiegl.nia.data

import dev.robinsiegl.nia.model.*
import dev.robinsiegl.nia.model.PreferencesValidation
import dev.robinsiegl.nia.model.SavedState
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
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
    private const val CURRENT_VERSION = 7
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
            if (version == null || version !in 1..CURRENT_VERSION) {
                throw BackupException("This backup version is not supported. Update nia and try again.")
            }
            if (root["profile"]?.jsonPrimitive?.content != "personal") {
                throw BackupException("This version of nia supports personal-profile backups only.")
            }
            if (version <= 6) validateLegacyTargets(root.getValue("settings").jsonObject)
            val compatible = if (version <= 5) JsonObject(root + ("settings" to migrateHome(migrateSettings(root.getValue("settings").jsonObject)))) else root
            val backup = json.decodeFromJsonElement<SettingsBackup>(compatible)
            PreferencesValidation.validate(backup.settings)
            return backup.settings
        } catch (e: BackupException) {
            throw e
        } catch (_: IllegalArgumentException) {
            throw BackupException("This settings file is invalid. Your current settings have not changed.")
        }
    }

    /** A missing list denotes legacy on-device state; an explicitly empty list stays empty. */
    fun decodeState(text: String): SavedState {
        val root = json.parseToJsonElement(text).jsonObject
        val preferences = root["preferences"]?.jsonObject ?: JsonObject(emptyMap())
        val migrated = migrateSettings(preferences).let { if ("homeShortcuts" in it) it else migrateHome(it) }
        val compatible = JsonObject(root + ("preferences" to migrated))
        return json.decodeFromJsonElement(compatible)
    }

    private fun migrateHome(settings: JsonObject): JsonObject {
        // Older schema versions cannot smuggle current-model fields past validation.
        require("homeShortcuts" !in settings && "calendar" !in settings)
        val assignments = settings["assignments"]?.let { json.decodeFromJsonElement<Map<HomeCategory, AppTarget>>(it) } ?: emptyMap()
        return JsonObject((settings - "assignments") + mapOf(
            "homeShortcuts" to json.encodeToJsonElement(legacyHomeShortcuts(assignments)),
            "calendar" to (assignments[HomeCategory.CALENDAR]?.let { json.encodeToJsonElement(it) } ?: JsonNull),
        ))
    }

    private fun validateLegacyTargets(settings: JsonObject) {
        settings["homeShortcuts"]?.jsonArray?.let { entries ->
            require(entries.size <= 8)
            entries.forEach { require("command" !in it.jsonObject) }
        }
        listOf("leftShortcut", "rightShortcut").forEach { key ->
            settings[key]?.jsonObject?.let { corner ->
                require("command" !in corner && corner["action"]?.jsonPrimitive?.content != "COMMAND")
            }
        }
    }

    private fun migrateSettings(settings: JsonObject): JsonObject = JsonObject(settings.mapValues { (key, value) ->
        if (key !in setOf("leftShortcut", "rightShortcut")) value
        else {
            val shortcut = value.jsonObject
            val action = shortcut["action"]?.jsonPrimitive?.content
            val icon = shortcut["icon"]?.jsonPrimitive?.content
            if (action == "SETTINGS") {
                require(shortcut["app"] == null || shortcut["app"] == JsonNull)
                JsonObject(shortcut + ("action" to JsonPrimitive("EMPTY")) +
                    if (icon == "SETTINGS") mapOf("icon" to JsonPrimitive("STAR")) else emptyMap())
            } else if (icon == "SETTINGS") JsonObject(shortcut + ("icon" to JsonPrimitive("STAR")))
            else shortcut
        }
    })
}
