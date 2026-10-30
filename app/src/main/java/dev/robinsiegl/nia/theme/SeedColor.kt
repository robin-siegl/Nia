package dev.robinsiegl.nia.theme

import kotlin.math.abs
import kotlin.math.roundToInt
import java.util.Locale

/** HSV coordinates for the picker; palette generation still happens in OKLCH. */
data class SeedColor(val hue: Float, val saturation: Float, val brightness: Float) {
    fun toArgb(): Int {
        val h = ((hue % 360f) + 360f) % 360f / 60f
        val chroma = brightness.coerceIn(0f, 1f) * saturation.coerceIn(0f, 1f)
        val x = chroma * (1f - abs(h % 2f - 1f))
        val (r, g, b) = when (h.toInt()) {
            0 -> Triple(chroma, x, 0f)
            1 -> Triple(x, chroma, 0f)
            2 -> Triple(0f, chroma, x)
            3 -> Triple(0f, x, chroma)
            4 -> Triple(x, 0f, chroma)
            else -> Triple(chroma, 0f, x)
        }
        val m = brightness.coerceIn(0f, 1f) - chroma
        return (0xFF shl 24) or (((r + m) * 255).roundToInt() shl 16) or
            (((g + m) * 255).roundToInt() shl 8) or ((b + m) * 255).roundToInt()
    }

    fun toHex(): String = String.format(Locale.ROOT, "#%06X", toArgb() and 0xFFFFFF)

    companion object {
        fun fromHex(hex: String): SeedColor {
            val rgb = hex.removePrefix("#").toInt(16)
            val r = ((rgb shr 16) and 255) / 255f
            val g = ((rgb shr 8) and 255) / 255f
            val b = (rgb and 255) / 255f
            val max = maxOf(r, g, b)
            val delta = max - minOf(r, g, b)
            val hue = when {
                delta == 0f -> 0f
                max == r -> 60f * (((g - b) / delta + 6f) % 6f)
                max == g -> 60f * ((b - r) / delta + 2f)
                else -> 60f * ((r - g) / delta + 4f)
            }
            return SeedColor(hue, if (max == 0f) 0f else delta / max, max)
        }
    }
}
