package dev.robinsiegl.nia.model

/** Unlocks are consumed only while nia is foreground; returning from another app is not an unlock. */
internal class UnlockTracker {
    private var armed = false
    val awaitingUnlock: Boolean get() = armed

    fun screenOff() { armed = true }
    fun userPresent(launcherResumed: Boolean) { if (!launcherResumed) armed = false }
    fun leaveForeground(locked: Boolean, interactive: Boolean) { if (!locked && interactive) armed = false }

    fun foreground(locked: Boolean, interactive: Boolean, focused: Boolean): Boolean {
        if (locked) { armed = true; return false }
        if (!interactive || !focused) return false
        return armed.also { armed = false }
    }
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
