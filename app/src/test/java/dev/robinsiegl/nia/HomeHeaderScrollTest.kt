package dev.robinsiegl.nia

import dev.robinsiegl.nia.model.HomeHeaderScroll
import org.junit.Assert.*
import org.junit.Test

class HomeHeaderScrollTest {
    @Test fun `header consumes only its own height change and leaves the rest for content`() {
        val header = HomeHeaderScroll()
        header.updateRange(100f)
        assertEquals(-40f, header.consume(-40f), 0f)
        assertEquals(0.4f, header.fraction, 0f)
        assertEquals(-60f, header.consume(-100f), 0f)
        assertEquals(1f, header.fraction, 0f)
        assertEquals(0f, header.consume(-20f), 0f)
        assertEquals(100f, header.consume(150f), 0f)
        assertEquals(0f, header.fraction, 0f)
    }
    @Test fun `a changed viewport clamps the header without an invalid or negative offset`() {
        val header = HomeHeaderScroll()
        header.updateRange(100f); header.consume(-80f)
        header.updateRange(40f)
        assertEquals(40f, header.offset, 0f)
        header.updateRange(0f)
        assertEquals(0f, header.fraction, 0f)
        assertEquals(0f, header.consume(-100f), 0f)
    }
}
