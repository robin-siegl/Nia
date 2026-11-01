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
            val p = LauncherPreferences(homeShortcuts = listOf(HomeShortcut("command", "My command", command.icon, command = command)),
                leftShortcut = CornerShortcut(ShortcutAction.COMMAND, command.icon, command = command),
                rightShortcut = CornerShortcut(ShortcutAction.COMMAND, ShortcutIcon.STAR, command = command))
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
        val smuggled = BackupCodec.export(command).decodeToString().replace("\"schemaVersion\": 7", "\"schemaVersion\": 6")
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
}
