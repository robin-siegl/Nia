package dev.robinsiegl.nia.theme

import dev.robinsiegl.nia.model.ColorStyle
import dev.robinsiegl.nia.model.LauncherPreferences
import dev.robinsiegl.nia.model.ThemeMode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

data class NiaPalette(val background: Int, val foreground: Int, val isDark: Boolean)

/**
 * One seed, two related tones. OKLab keeps perceived hue/lightness consistent; chroma is
 * reduced until the color fits sRGB. Matrices: https://bottosson.github.io/posts/oklab/
 * All generated pairs are checked against a 7:1 text contrast target.
 */
object Palette {
    fun generate(preferences: LauncherPreferences, systemDark: Boolean): NiaPalette {
        val dark = when (preferences.themeMode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        if (preferences.colorStyle == ColorStyle.MONOCHROME) return if (dark) {
            NiaPalette(0xFF000000.toInt(), 0xFFE6E6E6.toInt(), true)
        } else NiaPalette(0xFFFFFFFF.toInt(), 0xFF171717.toInt(), false)

        val seed = preferences.seedColor.removePrefix("#").toLong(16).toInt()
        val (hue, chroma) = hueChroma(seed)
        val background = if (dark && preferences.pureBlackColorBackground) 0xFF000000.toInt()
        else tone(if (dark) 0.16 else 0.955, minOf(chroma * 0.30, 0.04), hue)
        var lightness = if (dark) 0.89 else 0.25
        var foreground = tone(lightness, minOf(chroma * 0.55, 0.10), hue)
        // A finite fallback also covers black, white and colors at the sRGB boundary.
        repeat(20) {
            if (contrast(background, foreground) < 7.0) {
                lightness = (lightness + if (dark) 0.005 else -0.01).coerceIn(0.0, 1.0)
                foreground = tone(lightness, minOf(chroma * 0.55, 0.10), hue)
            }
        }
        if (contrast(background, foreground) < 7.0) foreground = if (dark) -1 else 0xFF000000.toInt()
        return NiaPalette(background, foreground, dark)
    }

    private fun linear(channel: Int): Double {
        val c = channel / 255.0
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(argb: Int): Double = 0.2126 * linear(argb shr 16 and 255) +
        0.7152 * linear(argb shr 8 and 255) + 0.0722 * linear(argb and 255)

    fun contrast(a: Int, b: Int): Double {
        val l1 = luminance(a); val l2 = luminance(b)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun hueChroma(rgb: Int): Pair<Double, Double> {
        val r = linear(rgb shr 16 and 255); val g = linear(rgb shr 8 and 255); val b = linear(rgb and 255)
        val l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
        val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        val chroma = hypot(a, bb)
        return atan2(bb, a) to if (chroma < 0.008) 0.0 else chroma
    }

    private fun rgb(lightness: Double, chroma: Double, hue: Double): DoubleArray {
        val a = cos(hue) * chroma; val b = sin(hue) * chroma
        val l = (lightness + 0.3963377774 * a + 0.2158037573 * b).pow(3)
        val m = (lightness - 0.1055613458 * a - 0.0638541728 * b).pow(3)
        val s = (lightness - 0.0894841775 * a - 1.2914855480 * b).pow(3)
        return doubleArrayOf(
            4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
        )
    }

    private fun tone(lightness: Double, chroma: Double, hue: Double): Int {
        var low = 0.0; var high = chroma
        repeat(16) {
            val mid = (low + high) / 2
            if (rgb(lightness, mid, hue).all { it in 0.0..1.0 }) low = mid else high = mid
        }
        fun channel(value: Double): Int {
            val v = value.coerceIn(0.0, 1.0)
            val srgb = if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1.0 / 2.4) - 0.055
            return (srgb * 255).roundToInt().coerceIn(0, 255)
        }
        val c = rgb(lightness, low, hue).map(::channel)
        return (255 shl 24) or (c[0] shl 16) or (c[1] shl 8) or c[2]
    }
}
