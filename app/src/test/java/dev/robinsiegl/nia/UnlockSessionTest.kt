package dev.robinsiegl.nia

import dev.robinsiegl.nia.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UnlockSessionTest {
    private val home = UnlockEnvironment(false, true, true)
    private val locked = UnlockEnvironment(true, false, false)

    private fun unlockedHome(): UnlockSession = UnlockSession().apply {
        initialize(home)
        resume(home, 0)
        pause(locked)
        screenOff(locked, 100)
        userPresent(home.copy(focused = false), 200)
        resume(home, 300)
        assertTrue(request!!.pending)
    }

    @Test fun `cold startup and ordinary focus changes do not invent an unlock`() {
        val session = UnlockSession()
        session.initialize(home)
        session.resume(home, 0)
        session.focusChanged(home.copy(focused = false), 50)
        session.focusChanged(home, 100)
        assertNull(session.request)
        assertTrue(session.active)
        assertFalse(session.deviceLocked)
    }

    @Test fun `resume pins an observed unlock while focus is delayed`() {
        val session = UnlockSession()
        session.initialize(home)
        session.resume(home, 0)
        session.pause(locked)
        session.stop(locked, false)
        session.userPresent(home.copy(focused = false), 100)
        session.resume(home.copy(focused = false), 200)
        assertNull(session.request)
        assertTrue(session.deviceLocked)
        session.focusChanged(home, 4_000)
        assertTrue(session.request!!.pending)
        assertTrue(session.active)
    }

    @Test fun `focus and resume in either order produce one request`() {
        for (focusFirst in listOf(true, false)) {
            val session = UnlockSession()
            session.initialize(locked)
            if (focusFirst) {
                session.focusChanged(home, 100)
                assertFalse(session.active)
                assertTrue(session.deviceLocked)
                session.resume(home, 200)
            } else {
                session.resume(home.copy(focused = false), 100)
                session.focusChanged(home, 200)
            }
            val request = session.request
            assertNotNull(request)
            session.userPresent(home, 300)
            session.refresh(home, 400)
            assertSame(request, session.request)
        }
    }

    @Test fun `focus loss requeues unfinished work even when no intermediate frame is drawn`() {
        val session = unlockedHome()
        val old = session.request!!
        session.focusChanged(home.copy(focused = false), 400)
        val replacement = session.request!!
        assertNotSame(old, replacement)
        assertFalse(session.active)
        old.consume()
        session.focusChanged(home, 500)
        assertSame(replacement, session.request)
        assertTrue(replacement.pending)
        replacement.consume()
        session.focusChanged(home.copy(focused = false), 600)
        session.focusChanged(home, 700)
        assertSame(replacement, session.request)
        assertFalse(replacement.pending)
    }

    @Test fun `configuration recreation retains the queued reveal`() {
        val session = unlockedHome()
        val old = session.request!!
        session.pause(home.copy(focused = false))
        val retained = session.request
        session.stop(home, changingConfigurations = true)
        session.initialize(home.copy(focused = false))
        old.consume()
        session.resume(home, 600)
        assertSame(retained, session.request)
        assertTrue(session.request!!.pending)
    }

    @Test fun `leaving for another app cancels an unfinished reveal`() {
        val session = unlockedHome()
        session.userLeaving(home)
        session.pause(home.copy(focused = false))
        session.stop(home, false)
        session.resume(home, 1_000)
        assertFalse(session.request!!.pending)
    }

    @Test fun `unlocking into a different app does not queue a later Home animation`() {
        val session = UnlockSession()
        session.initialize(home)
        session.resume(home, 0)
        session.pause(locked)
        session.stop(locked, false)
        session.userPresent(home.copy(focused = false), 100)
        session.resume(home, 4_000)
        assertNull(session.request)
    }

    @Test fun `deferred screen off cannot hide an already unlocked Home`() {
        val session = UnlockSession()
        session.initialize(home)
        session.resume(home, 0)
        session.screenOff(home, 100)
        assertFalse(session.deviceLocked)
        assertTrue(session.active)
        assertNull(session.request)
    }

    @Test fun `unlock into another nia page does not animate Home on a later return`() {
        val session = UnlockSession()
        session.initialize(home)
        session.resume(home, 0)
        session.pause(locked)
        session.resume(home.copy(homeVisible = false), 200)
        session.refresh(home, 300)
        assertNull(session.request)
    }

    @Test fun `recreating background Home behind a lock does not invent a new unlock owner`() {
        val session = unlockedHome()
        session.userLeaving(home)
        session.pause(home.copy(focused = false))
        session.stop(home, false)
        session.initialize(locked)
        session.userPresent(home.copy(focused = false), 1_000)
        session.resume(home, 2_000)
        assertFalse(session.request!!.pending)
    }
}
