package dev.robinsiegl.nia.model

/** Unlocks are consumed only while nia is foreground; returning from another app is not an unlock. */
internal class UnlockTracker {
    private var armed = false
    private var wasForeground = false
    private var backgroundDeadline: Long? = null
    val awaitingUnlock: Boolean get() = armed

    fun screenOff() {
        if (wasForeground) { armed = true; backgroundDeadline = null }
    }
    fun userPresent(launcherResumed: Boolean, nowMillis: Long = 0) {
        // USER_PRESENT can precede onStart/onResume. Allow that handoff, then expire it
        // if Android unlocked into a different app instead of retaining a stale reveal.
        if (armed && !launcherResumed) backgroundDeadline = nowMillis + 1_500
    }
    fun leaveForeground(locked: Boolean, interactive: Boolean) {
        if (!locked && interactive) { armed = false; wasForeground = false; backgroundDeadline = null }
        else if (wasForeground) { armed = true; backgroundDeadline = null }
    }

    fun foreground(locked: Boolean, interactive: Boolean, focused: Boolean, nowMillis: Long = 0): Boolean {
        if (locked || !interactive) { armed = true; wasForeground = true; backgroundDeadline = null; return false }
        if (backgroundDeadline?.let { nowMillis >= it } == true) armed = false
        if (!focused) return false
        wasForeground = true
        return armed.also { armed = false; backgroundDeadline = null }
    }
}

/** An event survives delayed settings loading but cannot replay when Home is remounted. */
class UnlockRevealRequest {
    var pending: Boolean = true
        private set
    fun consume(): Boolean = pending.also { pending = false }
}

/** Uses monotonic time; shared by all app/Clock launches to reject rapid duplicate taps. */
internal class LaunchGate(private val intervalMillis: Long = 450) {
    private var acceptedAt: Long? = null
    fun accept(now: Long): Boolean {
        if (acceptedAt?.let { now - it < intervalMillis } == true) return false
        acceptedAt = now
        return true
    }
    fun reset() { acceptedAt = null }
}
