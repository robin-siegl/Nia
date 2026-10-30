package dev.robinsiegl.nia

import dev.robinsiegl.nia.model.*
import org.junit.Assert.*
import org.junit.Test

class RestorePreviewTest {
    private val first = AppEntry(AppTarget("example.first", ".Main"), "First")
    private val second = AppEntry(AppTarget("example.second", ".Main"), "Second")
    private val apps = listOf(first, second)

    @Test fun `identical backups have no changes and no missing apps`() {
        val p = LauncherPreferences(assignments = mapOf(HomeCategory.MUSIC to first.target))
        val preview = RestorePreview.create(p, p, apps)
        assertTrue(preview.changes.isEmpty()); assertTrue(preview.unavailable.isEmpty())
        assertEquals(p, preview.preferences)
    }
    @Test fun `each changed preference and assignment has a before and after value`() {
        val original = LauncherPreferences()
        val requested = original.copy(layout = HomeLayout.LIST, homeTextSize = HomeTextSize.LARGE, pixelClockLayout = false,
            colorStyle = ColorStyle.COLOR, themeMode = ThemeMode.DARK, seedColor = "#112233", pureBlackColorBackground = true,
            motionMode = MotionMode.REDUCED, searchAutofocus = true, alphabetScrubber = true,
            assignments = HomeCategory.entries.associateWith { first.target }, clock = second.target,
            leftShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.BIKE, first.target),
            rightShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.MAIL, second.target))
        val preview = RestorePreview.create(original, requested, apps)
        assertEquals(20, preview.changes.size)
        assertEquals(requested, preview.preferences)
        assertTrue(preview.changes.all { it.before != it.after })
        assertEquals(setOf("Home", "Appearance", "Apps", "App assignments", "Corner shortcuts"), preview.changes.map { it.section }.toSet())
        assertTrue(preview.changes.first { it.label == "Music" }.after.contains("First"))
        assertTrue(preview.changes.first { it.label == "Clock" }.before.contains("System clock"))
    }
    @Test fun `cleared assignments are visible as actual changes`() {
        val original = LauncherPreferences(assignments = mapOf(HomeCategory.MUSIC to first.target), clock = first.target,
            leftShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.STAR, first.target))
        val preview = RestorePreview.create(original, LauncherPreferences(), apps)
        assertEquals(listOf("Music", "Clock", "Left shortcut"), preview.changes.map { it.label })
        assertEquals(listOf("Choose an app", "System clock", "Empty"), preview.changes.map { it.after })
    }
    @Test fun `unavailable apps are listed per assignment and sanitized exactly as previewed`() {
        val current = LauncherPreferences(assignments = mapOf(HomeCategory.MUSIC to first.target))
        val missing = AppTarget("example.missing", ".Old")
        val requested = current.copy(assignments = mapOf(HomeCategory.MUSIC to missing), clock = missing,
            rightShortcut = CornerShortcut(ShortcutAction.APP, ShortcutIcon.CLOUD, missing))
        val preview = RestorePreview.create(current, requested, apps)
        assertEquals(listOf("Music", "Clock", "Right shortcut"), preview.unavailable.map { it.label })
        assertTrue(preview.preferences.assignments.isEmpty())
        assertNull(preview.preferences.clock)
        assertEquals(CornerShortcut(icon = ShortcutIcon.CLOUD), preview.preferences.rightShortcut)
        assertEquals("Choose an app", preview.changes.first { it.label == "Music" }.after)
    }
    @Test fun `duplicate app labels retain the full activity identity in the preview`() {
        val renamed = second.copy(label = first.label)
        val preview = RestorePreview.create(LauncherPreferences(assignments = mapOf(HomeCategory.MUSIC to first.target)),
            LauncherPreferences(assignments = mapOf(HomeCategory.MUSIC to second.target)), listOf(first, renamed))
        val change = preview.changes.single()
        assertTrue(change.before.contains(first.target.key))
        assertTrue(change.after.contains(second.target.key))
    }
    @Test fun `an app becoming unavailable changes the exact restore result for review`() {
        val requested = LauncherPreferences(clock = first.target)
        val previous = RestorePreview.create(LauncherPreferences(), requested, apps)
        val fresh = RestorePreview.create(LauncherPreferences(), requested, listOf(second))
        assertNotEquals(previous.preferences, fresh.preferences)
        assertEquals(listOf("Clock"), fresh.unavailable.map { it.label })
    }
}
