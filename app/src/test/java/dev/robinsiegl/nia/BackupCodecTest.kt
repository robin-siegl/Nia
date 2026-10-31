package dev.robinsiegl.nia

import dev.robinsiegl.nia.data.BackupCodec
import dev.robinsiegl.nia.data.BackupException
import dev.robinsiegl.nia.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString

class BackupCodecTest {
    private val message = AppTarget("org.example.messages", "org.example.messages.MainActivity")
    private val calendar = AppTarget("org.example.calendar", ".Main")
    private val p = LauncherPreferences(layout = HomeLayout.LIST, colorStyle = ColorStyle.COLOR,
        themeMode = ThemeMode.SYSTEM, seedColor = "#F184AA", pureBlackColorBackground = true, searchAutofocus = true,
        homeTextSize = HomeTextSize.LARGE, alphabetScrubber = true, pixelClockLayout = false,
        motionMode = MotionMode.REDUCED,
        leftShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.NOTES, AppTarget("org.example.notes", ".Main")),
        rightShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.HEADPHONES, AppTarget("org.example.music", ".Main")),
        homeShortcuts = listOf(HomeShortcut("messages", "Chat", ShortcutIcon.MESSAGES, message)),
        clock = AppTarget("org.example.clock", ".ClockActivity"), calendar = calendar)
    private fun exported() = BackupCodec.export(p, "2026-10-30T09:00:00Z").decodeToString()
    private fun rejected(text: String) = assertThrows(BackupException::class.java) { BackupCodec.decode(text.encodeToByteArray()) }
    private fun legacySettings(version: Int): JsonObject {
        val missing = when (version) {
            1 -> setOf("homeTextSize", "alphabetScrubber", "pixelClockLayout", "motionMode", "leftShortcut", "rightShortcut")
            2 -> setOf("pixelClockLayout", "motionMode", "leftShortcut", "rightShortcut")
            3 -> setOf("motionMode", "leftShortcut", "rightShortcut")
            else -> emptySet()
        }
        return JsonObject((BackupCodec.json.parseToJsonElement(exported()).jsonObject.getValue("settings").jsonObject -
            (missing + setOf("homeShortcuts", "calendar"))) + ("assignments" to BackupCodec.json.encodeToJsonElement(
                mapOf(HomeCategory.MESSAGES to message, HomeCategory.CALENDAR to calendar))))
    }
    private fun legacyBackup(version: Int, settings: JsonObject = legacySettings(version)): String {
        val root = BackupCodec.json.parseToJsonElement(exported()).jsonObject
        return JsonObject(root + mapOf("settings" to settings, "schemaVersion" to JsonPrimitive(version))).toString()
    }

    @Test fun `schema six round trips independent home entries and header assignments`() {
        assertEquals(p, BackupCodec.decode(exported().encodeToByteArray()))
        assertTrue(exported().contains("\"schemaVersion\": 6"))
    }
    @Test fun `fresh defaults and deliberately empty Home remain empty after backup and device reload`() {
        val empty = LauncherPreferences(calendar = calendar)
        assertTrue(LauncherPreferences().homeShortcuts.isEmpty())
        assertEquals(empty, BackupCodec.decode(BackupCodec.export(empty)))
        assertEquals(empty, BackupCodec.decodeState(BackupCodec.json.encodeToString(SavedState(empty, true))).preferences)
    }
    @Test fun `all five legacy schemas migrate labels icons targets and independent calendar`() {
        for (version in 1..5) {
            val migrated = BackupCodec.decode(legacyBackup(version).encodeToByteArray())
            assertEquals(legacyHomeShortcuts(mapOf(HomeCategory.MESSAGES to message, HomeCategory.CALENDAR to calendar)), migrated.homeShortcuts)
            assertEquals(calendar, migrated.calendar)
            assertEquals(p.clock, migrated.clock)
            assertEquals(p.searchAutofocus, migrated.searchAutofocus)
            assertEquals(if (version <= 1) HomeTextSize.DEFAULT else p.homeTextSize, migrated.homeTextSize)
            assertEquals(if (version <= 2) true else p.pixelClockLayout, migrated.pixelClockLayout)
            assertEquals(if (version <= 3) MotionMode.SYSTEM else p.motionMode, migrated.motionMode)
            val state = BackupCodec.decodeState("""{"setupComplete":true,"setupStep":"APPS","preferences":${legacySettings(version)}}""")
            assertTrue(state.setupComplete)
            assertEquals(SetupStep.APPS, state.setupStep)
            assertEquals(migrated, state.preferences)
        }
    }
    @Test fun `a legacy empty assignments map still preserves five editable unassigned entries`() {
        val settings = JsonObject(legacySettings(5) + ("assignments" to JsonObject(emptyMap())))
        val migrated = BackupCodec.decodeState("""{"preferences":$settings,"setupComplete":true}""")
        assertEquals(legacyHomeShortcuts(), migrated.preferences.homeShortcuts)
        assertNull(migrated.preferences.calendar)
        assertTrue(migrated.setupComplete)
    }
    @Test fun `invalid corners enums and identities cannot enter preferences`() {
        assertThrows(IllegalArgumentException::class.java) { PreferencesValidation.validate(p.copy(leftShortcut = CornerShortcut(ShortcutAction.APP))) }
        assertThrows(IllegalArgumentException::class.java) { PreferencesValidation.validate(p.copy(leftShortcut = p.leftShortcut.copy(action = ShortcutAction.EMPTY))) }
        rejected(exported().replace(".Main\"", "intent://invalid\""))
        rejected(exported().replace("\"NOTES\"", "\"UNKNOWN_ICON\""))
        rejected(exported().replace("\"REDUCED\"", "\"UNKNOWN_MOTION\""))
    }
    @Test fun `home validation enforces size stable unique identities and readable names`() {
        val entry = p.homeShortcuts.single()
        val invalid = listOf(
            (0..8).map { entry.copy(id = "entry-$it") }, listOf(entry, entry),
            listOf(entry.copy(id = "../file")), listOf(entry.copy(name = "")), listOf(entry.copy(name = "bad\nname")),
            listOf(entry.copy(name = "x".repeat(81))), listOf(entry.copy(name = " Leading")),
        )
        invalid.forEach { entries ->
            assertThrows(IllegalArgumentException::class.java) { PreferencesValidation.validate(p.copy(homeShortcuts = entries)) }
        }
        val entries = (0..7).map { entry.copy(id = "entry-$it") }
        assertEquals(entries, BackupCodec.decode(BackupCodec.export(p.copy(homeShortcuts = entries))).homeShortcuts)
    }
    @Test fun `export contains no setup or runtime state`() {
        val text = exported()
        listOf("setupComplete", "isDefault", "query", "installedApps", "setupStep").forEach { assertFalse(text.contains(it)) }
    }
    @Test fun `setup step persists through saved state reload`() {
        for (step in SetupStep.entries) {
            val saved = SavedState(preferences = p, setupStep = step)
            assertEquals(saved, BackupCodec.decodeState(BackupCodec.json.encodeToString(saved)))
        }
    }
    @Test fun `other files future schemas and unsupported profiles are rejected`() {
        rejected("{}"); rejected(exported().replace("nia.settings", "other.settings"))
        rejected(exported().replace("\"schemaVersion\": 6", "\"schemaVersion\": 7"))
        rejected(exported().replace("\"personal\"", "\"private\""))
        rejected(exported().replace("\"personal\"", "\"work\""))
    }
    @Test fun `unknown fields and invalid settings are rejected even in legacy migrations`() {
        rejected(exported().replace("MESSAGES", "INSTAGRAM"))
        rejected(exported().replace("\"layout\": \"LIST\"", "\"layout\": \"LIST\", \"customOrder\": []"))
        rejected(exported().replace("org.example.messages.MainActivity", "intent://arbitrary"))
        rejected(exported().replace("#F184AA", "not-a-color"))
        rejected(exported().replace("\"homeTextSize\": \"LARGE\"", "\"homeTextSize\": \"GIANT\""))
        rejected(exported().replace("\"alphabetScrubber\": true", "\"alphabetScrubber\": \"yes\""))
        rejected(legacyBackup(5).replace("MESSAGES", "UNKNOWN"))
        rejected(legacyBackup(5, JsonObject(legacySettings(5) + ("homeShortcuts" to JsonArray(emptyList())))))
    }
    @Test fun `truncated malformed and oversized input is rejected`() {
        rejected(exported().dropLast(4)); rejected("[]"); rejected("not json")
        assertThrows(BackupException::class.java) { BackupCodec.decode(ByteArray(BackupCodec.MAX_BYTES + 1)) }
    }
    @Test fun `legacy Settings corners migrate without losing editable Home or setup progress`() {
        val legacyCorner = BackupCodec.json.parseToJsonElement("""{"action":"SETTINGS","icon":"SETTINGS"}""")
        val settings = JsonObject(legacySettings(4) + ("rightShortcut" to legacyCorner))
        val migrated = BackupCodec.decode(legacyBackup(4, settings).encodeToByteArray())
        assertEquals(CornerShortcut(), migrated.rightShortcut)
        assertEquals(5, migrated.homeShortcuts.size)
        val state = BackupCodec.decodeState("""{"preferences":$settings,"setupComplete":true,"setupStep":"DEFAULT"}""")
        assertEquals(SavedState(migrated, true, SetupStep.DEFAULT), state)
        assertFalse(BackupCodec.export(state.preferences).decodeToString().contains("SETTINGS"))
    }
    @Test fun `legacy gear on an app becomes a star and retains its target`() {
        val legacy = legacyBackup(4).replace("\"HEADPHONES\"", "\"SETTINGS\"")
        assertEquals(p.rightShortcut.copy(icon = ShortcutIcon.STAR), BackupCodec.decode(legacy.encodeToByteArray()).rightShortcut)
        rejected(exported().replace("\"HEADPHONES\"", "\"SETTINGS\""))
    }
}
