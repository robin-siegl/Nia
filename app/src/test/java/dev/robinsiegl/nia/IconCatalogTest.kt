package dev.robinsiegl.nia

import dev.robinsiegl.nia.data.BackupCodec
import dev.robinsiegl.nia.model.*
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class IconCatalogTest {
    @Test fun `search finds names synonyms accents punctuation and words in any order`() {
        assertTrue(ShortcutIcon.WIFI in IconCatalog.search("wi-fi"))
        assertTrue(ShortcutIcon.WIFI in IconCatalog.search("wifi"))
        assertTrue(ShortcutIcon.QR_CODE in IconCatalog.search("qrcode"))
        assertTrue(ShortcutIcon.WIFI in IconCatalog.search("WIRELESS"))
        assertTrue(ShortcutIcon.GAMING in IconCatalog.search("controller"))
        assertTrue(ShortcutIcon.MAPS in IconCatalog.search("gps"))
        assertTrue(ShortcutIcon.COFFEE in IconCatalog.search("café"))
        assertTrue(ShortcutIcon.NOTES in IconCatalog.search("writing notes"))
        assertEquals(IconCatalog.search("writing notes"), IconCatalog.search("notes writing"))
        assertTrue(IconCatalog.search("nothing-matches-this-xyz").isEmpty())
    }
    @Test fun `browse categories and search stay alphabetical without changing persisted identity`() {
        val all = IconCatalog.search("  ")
        assertEquals(ShortcutIcon.entries.toSet(), all.toSet())
        assertEquals(193, all.size)
        assertEquals(all.sortedBy { it.label.lowercase(java.util.Locale.ROOT) }, all)
        IconCategory.entries.forEach { group ->
            val icons = IconCatalog.search("", group)
            assertTrue(icons.isNotEmpty())
            assertTrue(icons.all { it.category == group })
        }
        assertTrue(IconCatalog.search("camera", IconCategory.TRAVEL).isEmpty())
    }
    @Test fun `every catalog icon survives Home and Quick corner backup round trips`() {
        val app = AppTarget("example.app", ".Main")
        ShortcutIcon.entries.forEach { icon ->
            val p = LauncherPreferences(homeShortcuts = listOf(HomeShortcut("home", icon.label, icon, app)),
                leftShortcut = CornerShortcut(ShortcutAction.APP, icon, app), rightShortcut = CornerShortcut(ShortcutAction.APP, icon, app))
            val exported = BackupCodec.export(p)
            assertEquals(p, BackupCodec.decode(exported))
            assertEquals(p, BackupCodec.decodeState(BackupCodec.json.encodeToString(SavedState(p, true))).preferences)
            assertFalse(exported.decodeToString().contains("keywords"))
        }
    }
    @Test fun `all twenty seven legacy IDs remain available and keep the same concepts`() {
        val legacy = listOf("STAR", "CAMERA", "BROWSER", "NOTES", "WALLET", "HEART", "PHONE", "MESSAGES", "MUSIC", "CALENDAR", "MAPS",
            "SEARCH", "MAIL", "PHOTOS", "FILES", "BOOK", "HEADPHONES", "PODCAST", "VIDEO", "SHOPPING", "TRAIN", "BIKE", "FITNESS",
            "COFFEE", "CALCULATOR", "CLOCK", "CLOUD")
        legacy.forEach { assertEquals(it, ShortcutIcon.valueOf(it).name) }
        assertEquals(listOf(ShortcutIcon.PHONE, ShortcutIcon.MESSAGES, ShortcutIcon.MUSIC, ShortcutIcon.CALENDAR, ShortcutIcon.MAPS),
            legacyHomeShortcuts().map { it.icon })
    }
    @Test fun `the new generic gear does not revive or collide with legacy Settings actions`() {
        assertTrue(ShortcutIcon.GEAR in IconCatalog.search("settings"))
        val app = AppTarget("example.app", ".Main")
        val p = LauncherPreferences(leftShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.GEAR, app))
        assertEquals(p, BackupCodec.decodeState(BackupCodec.json.encodeToString(SavedState(p, true))).preferences)
        val legacy = BackupCodec.json.encodeToString(SavedState(p, true)).replace("GEAR", "SETTINGS")
        assertEquals(ShortcutIcon.STAR, BackupCodec.decodeState(legacy).preferences.leftShortcut.icon)
        assertEquals(ShortcutAction.APP, BackupCodec.decodeState(legacy).preferences.leftShortcut.action)
    }
}
