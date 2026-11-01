package dev.robinsiegl.nia

import dev.robinsiegl.nia.data.BackupCodec
import dev.robinsiegl.nia.data.BackupException
import dev.robinsiegl.nia.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ShortcutCommandTest {
    private val app = AppTarget("example.app", ".Main")

    @Test fun `every command survives backup storage and restore without an installed app`() {
        ShortcutCommand.entries.forEach { command ->
            val minutes = if (command == ShortcutCommand.PRESET_TIMER) 15 else null
            val p = LauncherPreferences(homeShortcuts = listOf(HomeShortcut("command", "My command", command.icon, command = command, timerMinutes = minutes)),
                leftShortcut = CornerShortcut(ShortcutAction.COMMAND, command.icon, command = command, timerMinutes = minutes),
                rightShortcut = CornerShortcut(ShortcutAction.COMMAND, ShortcutIcon.STAR, command = command, timerMinutes = minutes))
            assertEquals(p, BackupCodec.decode(BackupCodec.export(p)))
            assertEquals(p, BackupCodec.decodeState(BackupCodec.json.encodeToString(SavedState(p, true))).preferences)
            val preview = RestorePreview.create(LauncherPreferences(), p, emptyList())
            assertEquals(p, preview.preferences)
            assertTrue(preview.unavailable.isEmpty())
            assertTrue(preview.changes.any { it.after.contains(command.label) })
        }
    }
    @Test fun `a shortcut has one explicit target and commands cannot contain app intents`() {
        val invalid = listOf(
            LauncherPreferences(homeShortcuts = listOf(HomeShortcut("both", "Both", app = app, command = ShortcutCommand.OPEN_APPS))),
            LauncherPreferences(leftShortcut = CornerShortcut(ShortcutAction.COMMAND)),
            LauncherPreferences(leftShortcut = CornerShortcut(ShortcutAction.COMMAND, app = app, command = ShortcutCommand.FLASHLIGHT)),
            LauncherPreferences(leftShortcut = CornerShortcut(ShortcutAction.APP, app = app, command = ShortcutCommand.OPEN_APPS)),
            LauncherPreferences(leftShortcut = CornerShortcut(command = ShortcutCommand.SEARCH_APPS)),
        )
        invalid.forEach { assertThrows(IllegalArgumentException::class.java) { PreferencesValidation.validate(it) } }
        val bytes = BackupCodec.export(LauncherPreferences(homeShortcuts = listOf(HomeShortcut("search", "Search", command = ShortcutCommand.SEARCH_APPS))))
        assertThrows(BackupException::class.java) { BackupCodec.decode(bytes.decodeToString().replace("SEARCH_APPS", "intent://arbitrary").encodeToByteArray()) }
    }
    @Test fun `schema six imports preserve the exact entries without inventing an Apps command`() {
        val p = LauncherPreferences(homeShortcuts = legacyHomeShortcuts())
        val root = BackupCodec.json.parseToJsonElement(BackupCodec.export(p).decodeToString()).jsonObject
        val legacy = JsonObject(root + ("schemaVersion" to JsonPrimitive(6))).toString()
        val imported = BackupCodec.decode(legacy.encodeToByteArray())
        assertEquals(p, imported)
        assertTrue(imported.homeShortcuts.none { it.command != null })
        val command = LauncherPreferences(homeShortcuts = listOf(HomeShortcut("apps", "Apps", command = ShortcutCommand.OPEN_APPS)))
        val smuggled = BackupCodec.export(command).decodeToString().replace("\"schemaVersion\": 8", "\"schemaVersion\": 6")
        assertThrows(BackupException::class.java) { BackupCodec.decode(smuggled.encodeToByteArray()) }
    }
    @Test fun `all nine entries are editable and stale orders cannot remove or duplicate entries`() {
        val p = LauncherPreferences(homeShortcuts = (1..9).map { HomeShortcut("id-$it", "Item $it", app = app) })
        val ids = p.homeShortcuts.map { it.id }
        assertEquals(ids.reversed(), p.reorderHome(ids.reversed()).homeShortcuts.map { it.id })
        assertEquals(p, p.reorderHome(ids.dropLast(1)))
        assertEquals(p, p.reorderHome(ids.dropLast(1) + ids.first()))
        assertEquals(p, p.reorderHome(ids.dropLast(1) + "new-entry"))
        assertEquals(p, BackupCodec.decode(BackupCodec.export(p)))
        assertThrows(IllegalArgumentException::class.java) { PreferencesValidation.validate(p.copy(homeShortcuts = p.homeShortcuts + HomeShortcut("ten", "Ten"))) }
    }
    @Test fun `restore describes command changes and only the actual configured order`() {
        val before = LauncherPreferences(homeShortcuts = listOf(HomeShortcut("first", "First", app = app)))
        val after = before.copy(homeShortcuts = listOf(before.homeShortcuts.first().copy(app = null, command = ShortcutCommand.FLASHLIGHT)))
        val preview = RestorePreview.create(before, after, listOf(AppEntry(app, "Example")))
        assertEquals("First · Action", preview.changes.single().label)
        assertEquals("Toggle flashlight", preview.changes.single().after)
        val removed = RestorePreview.create(after, LauncherPreferences(), emptyList())
        assertEquals("Empty", removed.changes.first { it.label == "Order" }.after)
        assertFalse(removed.changes.any { it.after.contains("→ Apps") })
    }
    @Test fun `preset durations round trip and every restore duration change is visible`() {
        val entry = HomeShortcut("tea", "Tea", command = ShortcutCommand.PRESET_TIMER, timerMinutes = 5)
        val corner = CornerShortcut(ShortcutAction.COMMAND, command = ShortcutCommand.PRESET_TIMER, timerMinutes = 15)
        val before = LauncherPreferences(homeShortcuts = listOf(entry), leftShortcut = corner, rightShortcut = corner.copy(timerMinutes = 25))
        val after = before.copy(homeShortcuts = listOf(entry.copy(timerMinutes = 1440)),
            leftShortcut = corner.copy(timerMinutes = 1), rightShortcut = corner.copy(timerMinutes = 5))
        assertEquals(after, BackupCodec.decode(BackupCodec.export(after)))
        assertEquals(after, BackupCodec.decodeState(BackupCodec.json.encodeToString(SavedState(after, true))).preferences)
        val preview = RestorePreview.create(before, after, emptyList())
        assertEquals(3, preview.changes.size)
        assertTrue(preview.changes.any { it.label == "Tea · Action" && it.before.endsWith("5 min") && it.after.endsWith("1440 min") })
        assertTrue(preview.changes.any { it.label == "Left shortcut" && it.before.endsWith("15 min") && it.after.endsWith("1 min") })
        assertTrue(preview.changes.any { it.label == "Right shortcut" && it.before.endsWith("25 min") && it.after.endsWith("5 min") })
        assertTrue(preview.unavailable.isEmpty())
    }
    @Test fun `invalid or unrelated timer options cannot enter saved settings or drafts`() {
        val preset = HomeShortcut("timer", "Timer", command = ShortcutCommand.PRESET_TIMER, timerMinutes = 5)
        listOf(null, 0, -1, 1441, Int.MAX_VALUE).forEach { minutes ->
            val invalid = preset.copy(timerMinutes = minutes)
            assertFalse(HomeShortcutDraft(invalid, true).canSave)
            assertThrows(IllegalArgumentException::class.java) { PreferencesValidation.validate(LauncherPreferences(homeShortcuts = listOf(invalid))) }
            assertThrows(IllegalArgumentException::class.java) { PreferencesValidation.validate(LauncherPreferences(
                leftShortcut = CornerShortcut(ShortcutAction.COMMAND, command = ShortcutCommand.PRESET_TIMER, timerMinutes = minutes))) }
        }
        listOf(null, ShortcutCommand.START_TIMER, ShortcutCommand.SET_ALARM, ShortcutCommand.TOGGLE_THEME).forEach { command ->
            assertThrows(IllegalArgumentException::class.java) { PreferencesValidation.validate(LauncherPreferences(
                homeShortcuts = listOf(preset.copy(command = command)))) }
        }
        val valid = BackupCodec.export(LauncherPreferences(homeShortcuts = listOf(preset))).decodeToString()
        listOf("0", "1441", "1.5", "\"15\"").forEach { invalid ->
            assertThrows(BackupException::class.java) { BackupCodec.decode(valid.replace("\"timerMinutes\": 5", "\"timerMinutes\": $invalid").encodeToByteArray()) }
        }
    }
    @Test fun `schema seven remains importable but cannot disguise new timer options`() {
        val original = LauncherPreferences(homeShortcuts = listOf(HomeShortcut("timer", "Timer", command = ShortcutCommand.START_TIMER)),
            rightShortcut = CornerShortcut(ShortcutAction.COMMAND, command = ShortcutCommand.FLASHLIGHT))
        val legacy = BackupCodec.export(original).decodeToString().replace("\"schemaVersion\": 8", "\"schemaVersion\": 7")
        assertEquals(original, BackupCodec.decode(legacy.encodeToByteArray()))
        val preset = original.copy(rightShortcut = CornerShortcut(ShortcutAction.COMMAND, command = ShortcutCommand.PRESET_TIMER, timerMinutes = 15))
        val smuggled = BackupCodec.export(preset).decodeToString().replace("\"schemaVersion\": 8", "\"schemaVersion\": 7")
        assertThrows(BackupException::class.java) { BackupCodec.decode(smuggled.encodeToByteArray()) }
    }
    @Test fun `preset editing preserves custom names and icons and discards unused parameters`() {
        val generated = HomeShortcut("new", "").withCommand(ShortcutCommand.PRESET_TIMER)
        assertEquals("5 min timer", generated.name)
        assertEquals(ShortcutIcon.TIMER, generated.icon)
        assertEquals(15, generated.withTimerDuration(15).timerMinutes)
        assertEquals("15 min timer", generated.withTimerDuration(15).name)
        val custom = generated.copy(name = "Tea", icon = ShortcutIcon.COFFEE).withTimerDuration(25)
        assertEquals("Tea", custom.name); assertEquals(ShortcutIcon.COFFEE, custom.icon)
        assertEquals(custom, custom.withCommand(ShortcutCommand.PRESET_TIMER))
        assertNull(custom.withCommand(ShortcutCommand.START_TIMER).timerMinutes)
        assertEquals("Tea", custom.withCommand(ShortcutCommand.SOUND_CONTROLS).name)
        val corner = CornerShortcut().withCommand(ShortcutCommand.PRESET_TIMER).copy(timerMinutes = 25)
        assertEquals(corner, corner.withCommand(ShortcutCommand.PRESET_TIMER))
        assertNull(corner.withCommand(ShortcutCommand.TOGGLE_THEME).timerMinutes)
    }
    @Test fun `theme toggle always selects the opposite visible theme`() {
        assertEquals(ThemeMode.DARK, ThemeMode.SYSTEM.toggled(false))
        assertEquals(ThemeMode.LIGHT, ThemeMode.SYSTEM.toggled(true))
        listOf(false, true).forEach { systemDark ->
            assertEquals(ThemeMode.DARK, ThemeMode.LIGHT.toggled(systemDark))
            assertEquals(ThemeMode.LIGHT, ThemeMode.DARK.toggled(systemDark))
        }
    }
}
