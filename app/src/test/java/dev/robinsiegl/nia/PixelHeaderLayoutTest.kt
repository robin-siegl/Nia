package dev.robinsiegl.nia

import dev.robinsiegl.nia.model.PixelHeaderLayout
import org.junit.Assert.*
import org.junit.Test

class PixelHeaderLayoutTest {
    @Test fun `reference capture anchors reproduce the measured lock screen`() {
        val header = PixelHeaderLayout.forWidth(688f, 0f, 77f)
        assertEquals(46f, header.clockLeft, 0.01f)
        assertEquals(186f, header.clockTop, 0.01f)
        assertEquals(89f, header.clockHeight, 0.01f)
        assertEquals(54f, header.dateLeft, 0.01f)
        assertEquals(350f, header.dateTop, 0.01f)
        assertEquals(23f, header.dateHeight, 0.01f)
        assertEquals(400f, header.height, 0.01f)
    }
    @Test fun `safe drawing inset is subtracted once so screen positions remain fixed`() {
        val header = PixelHeaderLayout.forWidth(688f, 64f, 77f)
        assertEquals(186f, header.clockTop + 64f, 0.01f)
        assertEquals(350f, header.dateTop + 64f, 0.01f)
    }
    @Test fun `anchors scale with screen resolution and keep the date below the clock`() {
        val scale = 1280f / 688f
        val header = PixelHeaderLayout.forWidth(1280f, 0f, 77f * scale)
        assertEquals(186f * scale, header.clockTop, 0.01f)
        assertEquals(350f * scale, header.dateTop, 0.01f)
        assertTrue(header.dateTop > header.clockTop + header.clockHeight)
    }
}
