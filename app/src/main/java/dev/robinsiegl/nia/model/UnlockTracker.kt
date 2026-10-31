package dev.robinsiegl.nia.model

/** Unlocks are consumed only while nia is foreground; returning from another app is not an unlock. */
internal class UnlockTracker {
    private var armed = false
    private var wasForeground = false
    private var backgroundDeadline: Long? = null
    private var userPresentReceived = false
    val awaitingUnlock: Boolean get() = armed

    fun userLeaving() { armed = false; wasForeground = false; backgroundDeadline = null; userPresentReceived = false }

    fun screenOff() {
        if (wasForeground) { armed = true; backgroundDeadline = null; userPresentReceived = false }
    }
    fun userPresent(launcherResumed: Boolean, nowMillis: Long = 0) {
        // USER_PRESENT can precede onStart/onResume. Allow that handoff, then expire it
        // if Android unlocked into a different app instead of retaining a stale reveal.
        userPresentReceived = true
        if (armed && !launcherResumed) backgroundDeadline = nowMillis + 1_500
    }
    fun leaveForeground(locked: Boolean, interactive: Boolean) {
        if (!locked && interactive) {
            // The stop from screen-off can arrive after the device already reports unlocked.
            // Preserve an observed lock until user-present or an explicit user departure.
            if (!armed || userPresentReceived) { armed = false; backgroundDeadline = null }
            wasForeground = false
        } else if (wasForeground && !armed) {
            armed = true; backgroundDeadline = null; userPresentReceived = false
        }
    }

    fun foreground(locked: Boolean, interactive: Boolean, focused: Boolean, nowMillis: Long = 0): Boolean {
        if (backgroundDeadline?.let { nowMillis >= it } == true) armed = false
        // This callback means nia has resumed. The grace period concerns background time,
        // not how long Android subsequently takes to give its window focus.
        backgroundDeadline = null
        wasForeground = true
        if (locked || !interactive) {
            if (!armed) { armed = true; userPresentReceived = false }
            return false
        }
        if (!focused) return false
        return armed.also { armed = false; backgroundDeadline = null }
    }
}

/** Remains pending until visible completion or deliberate cancellation. */
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
