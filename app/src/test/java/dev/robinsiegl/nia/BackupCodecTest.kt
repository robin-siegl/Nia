package dev.robinsiegl.nia

import dev.robinsiegl.nia.data.BackupCodec
import dev.robinsiegl.nia.data.BackupException
import dev.robinsiegl.nia.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.encodeToString

class BackupCodecTest {
    private val p = LauncherPreferences(layout = HomeLayout.LIST, colorStyle = ColorStyle.COLOR,
        themeMode = ThemeMode.SYSTEM, seedColor = "#F184AA", pureBlackColorBackground = true, searchAutofocus = true,
        homeTextSize = HomeTextSize.LARGE, alphabetScrubber = true,
        assignments = mapOf(HomeCategory.MESSAGES to AppTarget("org.example.messages", "org.example.messages.MainActivity")),
        clock = AppTarget("org.example.clock", ".ClockActivity"))
    private fun exported() = BackupCodec.export(p, "2026-10-30T09:00:00Z").decodeToString()
    private fun rejected(text: String) = assertThrows(BackupException::class.java) { BackupCodec.decode(text.encodeToByteArray()) }

    @Test fun `all user preferences and assignments round trip`() {
        assertEquals(p, BackupCodec.decode(exported().encodeToByteArray()))
    }
    @Test fun `older backups and saved settings use defaults for the new preferences`() {
        val root = BackupCodec.json.parseToJsonElement(exported()).jsonObject
        val legacy = JsonObject(root.getValue("settings").jsonObject.filterKeys { it !in setOf("homeTextSize", "alphabetScrubber") })
        val backup = JsonObject(root + mapOf("settings" to legacy, "schemaVersion" to JsonPrimitive(1))).toString().encodeToByteArray()
        val expected = p.copy(homeTextSize = HomeTextSize.DEFAULT, alphabetScrubber = false)
        assertEquals(expected, BackupCodec.decode(backup))
        val saved = """{"setupComplete":true,"preferences":$legacy}"""
        assertEquals(SavedState(expected, setupComplete = true), BackupCodec.json.decodeFromString<SavedState>(saved))
    }
    @Test fun `export contains no setup or runtime state`() {
        val text = exported()
        assertFalse(text.contains("setupComplete")); assertFalse(text.contains("isDefault"))
        assertFalse(text.contains("query")); assertFalse(text.contains("installedApps"))
        assertFalse(text.contains("setupStep"))
    }
    @Test fun `setup step persists through saved state reload`() {
        for (step in SetupStep.entries) {
            val saved = SavedState(preferences = p, setupStep = step)
            assertEquals(saved, BackupCodec.json.decodeFromString<SavedState>(BackupCodec.json.encodeToString(saved)))
        }
    }
    @Test fun `other files and future schemas are rejected`() {
        rejected("{}"); rejected(exported().replace("nia.settings", "other.settings"))
        rejected(exported().replace("\"schemaVersion\": 2", "\"schemaVersion\": 3"))
    }
    @Test fun `private and work profile backups are rejected`() {
        rejected(exported().replace("\"personal\"", "\"private\""))
        rejected(exported().replace("\"personal\"", "\"work\""))
    }
    @Test fun `unknown categories fields and invalid app targets are rejected`() {
        rejected(exported().replace("MESSAGES", "INSTAGRAM"))
        rejected(exported().replace("\"layout\": \"LIST\"", "\"layout\": \"LIST\", \"customOrder\": []"))
        rejected(exported().replace("org.example.messages.MainActivity", "intent://arbitrary"))
        rejected(exported().replace("#F184AA", "not-a-color"))
        rejected(exported().replace("\"homeTextSize\": \"LARGE\"", "\"homeTextSize\": \"GIANT\""))
        rejected(exported().replace("\"alphabetScrubber\": true", "\"alphabetScrubber\": \"yes\""))
    }
    @Test fun `truncated malformed and oversized input is rejected`() {
        rejected(exported().dropLast(4)); rejected("[]"); rejected("not json")
        assertThrows(BackupException::class.java) { BackupCodec.decode(ByteArray(BackupCodec.MAX_BYTES + 1)) }
    }
}
