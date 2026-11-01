package dev.robinsiegl.nia.model

import androidx.compose.runtime.*

internal enum class PullCue { ARMED }

/** Direct finger distance only. Momentum never calls this state machine. */
@Stable
internal class AppsPull {
    var distance by mutableFloatStateOf(0f)
        private set
    var armed by mutableStateOf(false)
        private set
    val active: Boolean get() = distance > 0f
    val preview: Float get() = 0.6f * distance / (threshold + distance)
    private var threshold = 1f
    private var announcedArm = false

    fun drag(deltaY: Float, thresholdPx: Float, cue: (PullCue) -> Unit): Float {
        threshold = thresholdPx.coerceAtLeast(1f)
        val before = distance
        distance = (distance - deltaY).coerceIn(0f, threshold * 4f)
        if (!armed && distance >= threshold) {
            armed = true
            if (!announcedArm) { announcedArm = true; cue(PullCue.ARMED) }
        } else if (armed && distance < threshold * 0.72f) armed = false
        return before - distance
    }

    fun reset() { distance = 0f; armed = false; announcedArm = false }
}
