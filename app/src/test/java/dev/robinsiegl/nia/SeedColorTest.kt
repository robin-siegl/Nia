package dev.robinsiegl.nia

import dev.robinsiegl.nia.model.ColorStyle
import dev.robinsiegl.nia.model.LauncherPreferences
import dev.robinsiegl.nia.theme.Palette
import dev.robinsiegl.nia.theme.SeedColor
import org.junit.Assert.*
import org.junit.Test

class SeedColorTest {
    @Test fun `picker hue positions match the displayed color wheel`() {
        listOf("#FF0000", "#FFFF00", "#00FF00", "#00FFFF", "#0000FF", "#FF00FF").forEachIndexed { i, hex ->
            assertEquals(hex, SeedColor(i * 60f, 1f, 1f).toHex())
        }
        assertEquals("#FF0000", SeedColor(360f, 1f, 1f).toHex())
        assertEquals("#FF00FF", SeedColor(-60f, 1f, 1f).toHex())
    }
    @Test fun `hex conversion preserves RGB colors including neutral and dark colors`() {
        for (r in 0..255 step 17) for (g in 0..255 step 17) for (b in 0..255 step 17) {
            val rgb = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            val hex = "#%06X".format(rgb and 0xFFFFFF)
            val seed = SeedColor.fromHex(hex)
            assertEquals(hex, seed.toHex())
            assertTrue(seed.hue.isFinite() && seed.saturation.isFinite() && seed.brightness.isFinite())
        }
    }
    @Test fun `wheel colors produce readable light and dark palette pairs`() {
        for (h in 0..330 step 30) for (s in listOf(0f, 0.5f, 1f)) for (v in listOf(0f, 0.5f, 1f)) {
            val p = LauncherPreferences(colorStyle = ColorStyle.COLOR, seedColor = SeedColor(h.toFloat(), s, v).toHex())
            for (dark in listOf(false, true)) {
                val palette = Palette.generate(p, dark)
                assertTrue(Palette.contrast(palette.background, palette.foreground) >= 7.0)
            }
        }
    }
}
