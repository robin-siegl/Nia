package dev.robinsiegl.nia

import dev.robinsiegl.nia.model.*
import org.junit.Assert.*
import org.junit.Test

class RestorePreviewTest {
    private val first = AppEntry(AppTarget("example.first", ".Main"), "First")
    private val second = AppEntry(AppTarget("example.second", ".Main"), "Second")
    private val apps = listOf(first, second)
    private fun home(app: AppTarget? = first.target) = listOf(HomeShortcut("music", "Music", ShortcutIcon.MUSIC, app))

    @Test fun `identical backups have no changes or missing apps`() {
        val p = LauncherPreferences(homeShortcuts = home())
        val preview = RestorePreview.create(p, p, apps)
        assertTrue(preview.changes.isEmpty()); assertTrue(preview.unavailable.isEmpty())
        assertEquals(p, preview.preferences)
    }
    @Test fun `every preference and independent header target is previewed`() {
        val original = LauncherPreferences()
        val requested = original.copy(layout = HomeLayout.LIST, homeTextSize = HomeTextSize.LARGE, pixelClockLayout = false,
            colorStyle = ColorStyle.COLOR, themeMode = ThemeMode.DARK, seedColor = "#112233", pureBlackColorBackground = true,
            motionMode = MotionMode.REDUCED, searchAutofocus = true, alphabetScrubber = true,
            homeShortcuts = home(), clock = second.target, calendar = first.target,
            leftShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.BIKE, first.target),
            rightShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.MAIL, second.target))
        val preview = RestorePreview.create(original, requested, apps)
        assertEquals(18, preview.changes.size)
        assertEquals(requested, preview.preferences)
        assertTrue(preview.changes.all { it.before != it.after })
        assertEquals(setOf("Home screen", "Appearance", "App list", "Home apps", "Clock & date", "Corner shortcuts"), preview.changes.map { it.section }.toSet())
        assertTrue(preview.changes.first { it.label == "Music" }.after.contains("Music icon"))
        assertTrue(preview.changes.first { it.label == "Clock" }.before.contains("System clock"))
        assertTrue(preview.changes.first { it.label == "Calendar" }.after.contains("First"))
    }
    @Test fun `removed Home entries and cleared assignments are shown`() {
        val original = LauncherPreferences(homeShortcuts = home(), clock = first.target,
            leftShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.STAR, first.target))
        val preview = RestorePreview.create(original, LauncherPreferences(), apps)
        assertEquals(listOf("Music", "Order", "Clock", "Left shortcut"), preview.changes.map { it.label })
        assertEquals(listOf("Not on Home", "Apps", "System clock", "Empty"), preview.changes.map { it.after })
    }
    @Test fun `unavailable targets clear assignments but preserve each Home entry and icon`() {
        val current = LauncherPreferences(homeShortcuts = home())
        val missing = AppTarget("example.missing", ".Old")
        val requested = current.copy(homeShortcuts = home(missing), clock = missing, calendar = missing,
            rightShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.CLOUD, missing))
        val preview = RestorePreview.create(current, requested, apps)
        assertEquals(listOf("Music", "Calendar", "Clock", "Right shortcut"), preview.unavailable.map { it.label })
        assertEquals(home(null), preview.preferences.homeShortcuts)
        assertNull(preview.preferences.clock); assertNull(preview.preferences.calendar)
        assertEquals(CornerShortcut(icon = ShortcutIcon.CLOUD), preview.preferences.rightShortcut)
        assertEquals("Choose an app", preview.changes.first { it.label == "Music · App" }.after)
    }
    @Test fun `renaming changing icons reassigning and reordering have separate exact previews`() {
        val original = LauncherPreferences(homeShortcuts = home() + HomeShortcut("map", "Maps", ShortcutIcon.MAPS, first.target))
        val requested = original.copy(homeShortcuts = listOf(original.homeShortcuts[1], original.homeShortcuts[0].copy(
            name = "My music", icon = ShortcutIcon.HEADPHONES, app = second.target)))
        val changes = RestorePreview.create(original, requested, apps).changes
        assertEquals(listOf("Music · Name", "My music · Icon", "My music · App", "Order"), changes.map { it.label })
        assertEquals("Maps → My music → Apps", changes.last().after)
    }
    @Test fun `duplicate app labels retain full activity identities`() {
        val renamed = second.copy(label = first.label)
        val preview = RestorePreview.create(LauncherPreferences(homeShortcuts = home()),
            LauncherPreferences(homeShortcuts = home(second.target)), listOf(first, renamed))
        val change = preview.changes.single()
        assertTrue(change.before.contains(first.target.key)); assertTrue(change.after.contains(second.target.key))
    }
    @Test fun `availability changes require a different review result`() {
        val requested = LauncherPreferences(clock = first.target)
        val previous = RestorePreview.create(LauncherPreferences(), requested, apps)
        val fresh = RestorePreview.create(LauncherPreferences(), requested, listOf(second))
        assertNotEquals(previous.preferences, fresh.preferences)
        assertEquals(listOf("Clock"), fresh.unavailable.map { it.label })
    }
}
