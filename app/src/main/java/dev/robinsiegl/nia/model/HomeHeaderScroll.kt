package dev.robinsiegl.nia.model

import androidx.compose.runtime.*

/** Header height is consumed before Home entries scroll; the same offset moves both. */
@Stable
internal class HomeHeaderScroll {
    var offset by mutableFloatStateOf(0f)
        private set
    var range by mutableFloatStateOf(0f)
        private set
    val fraction: Float get() = if (range > 0f) (offset / range).coerceIn(0f, 1f) else 0f

    fun updateRange(value: Float) { range = value.coerceAtLeast(0f); offset = offset.coerceIn(0f, range) }
    fun consume(deltaY: Float): Float {
        val before = offset
        offset = (offset - deltaY).coerceIn(0f, range)
        return before - offset
    }
}
