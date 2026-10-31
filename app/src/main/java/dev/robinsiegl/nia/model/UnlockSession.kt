package dev.robinsiegl.nia.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal data class UnlockEnvironment(
    val locked: Boolean, val interactive: Boolean, val focused: Boolean, val homeVisible: Boolean = true,
)

/** Retained by the ViewModel; activity recreation must not lose an unfinished reveal. */
internal class UnlockSession {
    private val tracker = UnlockTracker()
    private var resumed = false
    private var initialized = false
    var request by mutableStateOf<UnlockRevealRequest?>(null)
        private set
    var active by mutableStateOf(false)
        private set
    var deviceLocked by mutableStateOf(false)
        private set

    fun initialize(environment: UnlockEnvironment) {
        resumed = false
        active = false
        deviceLocked = environment.locked || !environment.interactive
        if (!initialized && deviceLocked && environment.homeVisible) {
            tracker.foreground(environment.locked, environment.interactive, false)
        }
        initialized = true
    }

    fun resume(environment: UnlockEnvironment, nowMillis: Long) {
        resumed = true
        refresh(environment, nowMillis)
    }

    fun focusChanged(environment: UnlockEnvironment, nowMillis: Long) {
        if (!environment.focused) requeueUnfinished()
        refresh(environment, nowMillis)
    }

    fun pause(environment: UnlockEnvironment) {
        resumed = false
        active = false
        if (environment.locked || !environment.interactive) {
            tracker.screenOff()
            request?.consume()
            deviceLocked = true
        } else requeueUnfinished()
    }

    fun stop(environment: UnlockEnvironment, changingConfigurations: Boolean) {
        if (changingConfigurations) return
        tracker.leaveForeground(environment.locked, environment.interactive)
        if (!environment.locked && environment.interactive) request?.consume()
    }

    fun userLeaving(environment: UnlockEnvironment) {
        if (!environment.locked && environment.interactive) {
            tracker.userLeaving()
            request?.consume()
        }
    }

    fun screenOff(environment: UnlockEnvironment, nowMillis: Long) {
        // Android can defer cached broadcasts. A historical SCREEN_OFF is not a current lock.
        if (!environment.locked && environment.interactive) {
            refresh(environment, nowMillis)
            return
        }
        tracker.screenOff()
        request?.consume()
        active = false
        deviceLocked = true
    }

    fun userPresent(environment: UnlockEnvironment, nowMillis: Long) {
        tracker.userPresent(resumed, nowMillis)
        refresh(environment, nowMillis)
    }

    fun refresh(environment: UnlockEnvironment, nowMillis: Long) {
        if (!environment.homeVisible) request?.consume()
        if (resumed && tracker.foreground(environment.locked, environment.interactive, environment.focused, nowMillis)) {
            request?.consume()
            request = if (environment.homeVisible) UnlockRevealRequest() else null
        }
        active = resumed && environment.focused && environment.interactive && !environment.locked
        deviceLocked = environment.locked || !environment.interactive || (tracker.awaitingUnlock && !active)
    }

    private fun requeueUnfinished() {
        // A new identity also restarts an effect when pause/resume coalesce while frames stop.
        // Completion of the old effect cannot consume the retained replacement request.
        if (request?.pending == true) request = UnlockRevealRequest()
    }
}
