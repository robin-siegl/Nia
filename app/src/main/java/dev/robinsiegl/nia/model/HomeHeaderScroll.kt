package dev.robinsiegl.nia.model

import androidx.compose.runtime.*

/** Scrolled Home keeps its offset; a fitting Home previews contraction with the Apps pull. */
@Stable
internal class HomeHeaderScroll {
    var offset by mutableFloatStateOf(0f)
        private set
    var range by mutableFloatStateOf(0f)
        private set
    private var pulling by mutableStateOf(false)
    val fraction: Float get() = if (range > 0f) (offset / range).coerceIn(0f, 1f) else 0f

    fun beginPull() { pulling = true }
    fun fractionWithPull(pullFraction: Float): Float =
        if (pulling) fraction + (1f - fraction) * pullFraction.coerceIn(0f, 1f) else fraction

    /** Save compaction on opening; cancellation returns to the unchanged scroll offset. */
    fun finishPull(open: Boolean) {
        if (pulling && open) offset = range
        pulling = false
    }

    fun updateRange(value: Float) { range = value.coerceAtLeast(0f); offset = offset.coerceIn(0f, range) }
    fun consume(deltaY: Float): Float {
        val before = offset
        offset = (offset - deltaY).coerceIn(0f, range)
        return before - offset
    }
}
