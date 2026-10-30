package dev.robinsiegl.nia.model

import kotlin.math.max

/** Portrait header anchors measured from the supplied 688 × 1536 Pixel lock-screen capture. */
data class PixelHeaderLayout(
    val clockLeft: Float, val clockTop: Float, val clockHeight: Float, val clockSampleWidth: Float,
    val dateLeft: Float, val dateTop: Float, val dateHeight: Float, val dateSampleWidth: Float,
    val height: Float,
) {
    companion object {
        fun forWidth(width: Float, contentTopInset: Float, minimumTouch: Float): PixelHeaderLayout {
            val scale = width / 688f
            val clockHeight = 89f * scale
            val dateHeight = 23f * scale
            val clockTop = max(0f, 186f * scale - contentTopInset)
            val dateTop = max(350f * scale - contentTopInset, clockTop + clockHeight + minimumTouch / 4f)
            return PixelHeaderLayout(46f * scale, clockTop, clockHeight, 272f * scale,
                54f * scale, dateTop, dateHeight, 126f * scale,
                dateTop + dateHeight + max(0f, (minimumTouch - dateHeight) / 2f))
        }
    }
}
