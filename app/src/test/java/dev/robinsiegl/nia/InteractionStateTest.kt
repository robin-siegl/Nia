package dev.robinsiegl.nia

import dev.robinsiegl.nia.model.*
import dev.robinsiegl.nia.ui.drawerShouldOpen
import org.junit.Assert.*
import org.junit.Test

class InteractionStateTest {
    @Test fun `ordinary cold start and app returns do not count as unlocks`() {
        val tracker = UnlockTracker()
        assertFalse(tracker.foreground(false, true, true))
        tracker.leaveForeground(false, true)
        assertFalse(tracker.foreground(false, true, true))
    }
    @Test fun `unlock waits for an interactive focused launcher and is consumed once`() {
        val tracker = UnlockTracker()
        tracker.foreground(false, true, true)
        tracker.screenOff()
        assertFalse(tracker.foreground(true, false, false))
        assertFalse(tracker.foreground(false, true, false))
        assertTrue(tracker.awaitingUnlock)
        tracker.userPresent(true)
        assertTrue(tracker.foreground(false, true, true))
        assertFalse(tracker.awaitingUnlock)
        tracker.userPresent(true)
        assertFalse(tracker.foreground(false, true, true))
    }
    @Test fun `unlocking into another app expires the pending Home reveal`() {
        val tracker = UnlockTracker()
        tracker.foreground(false, true, true)
        tracker.screenOff()
        tracker.userPresent(false, 100)
        assertFalse(tracker.foreground(false, true, true, 1600))
    }
    @Test fun `screen off pause retains unlock but leaving for an app clears it`() {
        val tracker = UnlockTracker()
        tracker.foreground(false, true, true)
        tracker.screenOff()
        tracker.leaveForeground(false, false)
        assertTrue(tracker.foreground(false, true, true))
        tracker.screenOff()
        tracker.leaveForeground(false, true)
        assertFalse(tracker.foreground(false, true, true))
    }
    @Test fun `starting behind keyguard can reveal on its first actual unlock`() {
        val tracker = UnlockTracker()
        assertFalse(tracker.foreground(true, true, true))
        assertTrue(tracker.foreground(false, true, true))
    }
    @Test fun `launch guard rejects duplicate rapid taps and allows a later tap`() {
        val gate = LaunchGate()
        assertTrue(gate.accept(0))
        assertFalse(gate.accept(1)); assertFalse(gate.accept(449))
        assertTrue(gate.accept(450))
        gate.reset()
        assertTrue(gate.accept(451))
    }
    @Test fun `drawer release uses gesture direction or the nearest resting position`() {
        assertTrue(drawerShouldOpen(0.1f, -900f, 700f))
        assertFalse(drawerShouldOpen(0.9f, 900f, 700f))
        assertFalse(drawerShouldOpen(0.49f, 0f, 700f))
        assertTrue(drawerShouldOpen(0.51f, 0f, 700f))
    }
    @Test fun `changing one corner keeps the other corner and fixed assignments intact`() {
        val right = CornerShortcut(ShortcutAction.APP, ShortcutIcon.HEADPHONES, AppTarget("example.podcast", ".Main"))
        val p = LauncherPreferences(rightShortcut = right, assignments = mapOf(HomeCategory.MUSIC to AppTarget("example.music", ".Main")))
        val left = CornerShortcut(ShortcutAction.APP, ShortcutIcon.CAMERA, AppTarget("example.camera", ".Main"))
        val changed = p.withShortcut(CornerPosition.LEFT, left)
        assertEquals(left, changed.shortcut(CornerPosition.LEFT))
        assertEquals(right, changed.shortcut(CornerPosition.RIGHT))
        assertEquals(p.assignments, changed.assignments)
    }

    @Test fun `user present before onResume retains the actual Home unlock`() {
        val tracker = UnlockTracker()
        tracker.foreground(false, true, true)
        tracker.leaveForeground(true, false)
        tracker.screenOff()
        tracker.userPresent(false, 200)
        assertFalse(tracker.foreground(false, true, false, 240))
        assertTrue(tracker.foreground(false, true, true, 280))
        assertFalse(tracker.foreground(false, true, true, 300))
    }

    @Test fun `screen off while a different app is foreground does not animate a later app return`() {
        val tracker = UnlockTracker()
        tracker.foreground(false, true, true)
        tracker.leaveForeground(false, true)
        tracker.screenOff()
        tracker.userPresent(false, 100)
        assertFalse(tracker.foreground(false, true, true, 150))
    }

    @Test fun `reveal event is consumed once across Home recreation`() {
        val request = UnlockRevealRequest()
        assertTrue(request.pending)
        assertTrue(request.consume())
        assertFalse(request.pending)
        assertFalse(request.consume())
    }
}
