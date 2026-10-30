package dev.robinsiegl.nia

import dev.robinsiegl.nia.model.*
import dev.robinsiegl.nia.theme.Palette
import org.junit.Assert.*
import org.junit.Test

class PaletteTest {
    @Test fun `monochrome dark is always pure OLED black`() {
        val p = LauncherPreferences(themeMode = ThemeMode.DARK)
        assertEquals(0xFF000000.toInt(), Palette.generate(p, false).background)
        assertEquals(0xFFFFFFFF.toInt(), Palette.generate(p.copy(themeMode = ThemeMode.LIGHT), true).background)
    }

    @Test fun `system mode follows system while explicit mode overrides it`() {
        val p = LauncherPreferences()
        assertFalse(Palette.generate(p, false).isDark)
        assertTrue(Palette.generate(p, true).isDark)
        assertFalse(Palette.generate(p.copy(themeMode = ThemeMode.LIGHT), true).isDark)
        assertTrue(Palette.generate(p.copy(themeMode = ThemeMode.DARK), false).isDark)
    }

    @Test fun `color pairs have strong contrast across the RGB gamut`() {
        for (r in 0..255 step 51) for (g in 0..255 step 51) for (b in 0..255 step 51) {
            val seed = "#%02X%02X%02X".format(r, g, b)
            for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
                val p = LauncherPreferences(colorStyle = ColorStyle.COLOR, themeMode = mode, seedColor = seed)
                val pair = Palette.generate(p, false)
                assertTrue("$seed $mode", Palette.contrast(pair.background, pair.foreground) >= 7.0)
                assertEquals(255, pair.background ushr 24)
                assertEquals(255, pair.foreground ushr 24)
            }
        }
    }

    @Test fun `violet creates a light pastel and a related dark tone`() {
        val p = LauncherPreferences(colorStyle = ColorStyle.COLOR, themeMode = ThemeMode.LIGHT)
        val light = Palette.generate(p, false)
        val dark = Palette.generate(p.copy(themeMode = ThemeMode.DARK), false)
        assertTrue((light.background and 255) > 220)
        assertTrue((light.foreground and 255) > (light.foreground shr 8 and 255))
        assertTrue((dark.background and 255) < 100)
        assertTrue((dark.foreground and 255) > 180)
        assertNotEquals(0xFF000000.toInt(), dark.background)
    }

    @Test fun `colored OLED mode retains colored foreground on pure black`() {
        val p = LauncherPreferences(colorStyle = ColorStyle.COLOR, themeMode = ThemeMode.DARK, pureBlackColorBackground = true)
        val pair = Palette.generate(p, false)
        assertEquals(0xFF000000.toInt(), pair.background)
        assertNotEquals(0xFFE6E6E6.toInt(), pair.foreground)
        assertTrue(Palette.contrast(pair.background, pair.foreground) >= 7)
    }
}
