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

    @Test fun `a fitting Home previews with the drawer and rolls back to its original offset`() {
        val header = HomeHeaderScroll()
        header.updateRange(100f); header.consume(-20f)
        header.beginPull()
        assertEquals(0.2f, header.fractionWithPull(0f), 0.001f)
        assertEquals(0.6f, header.fractionWithPull(0.5f), 0.001f)
        assertEquals(1f, header.fractionWithPull(2f), 0f)
        assertEquals(20f, header.offset, 0f)
        // Reversal follows the same path, without committing a new scroll position.
        assertEquals(0.4f, header.fractionWithPull(0.25f), 0.001f)
        header.finishPull(false)
        assertEquals(0.2f, header.fractionWithPull(1f), 0.001f)
        assertEquals(20f, header.offset, 0f)
    }

    @Test fun `fitting Home stays coupled while open and re-expands on return`() {
        val header = HomeHeaderScroll()
        header.updateRange(100f); header.beginPull()
        assertEquals(0.5f, header.fractionWithPull(0.5f), 0f)
        header.updateRange(40f)
        assertEquals(0.5f, header.fractionWithPull(0.5f), 0f)
        header.finishPull(true)
        assertEquals(0f, header.offset, 0f)
        assertEquals(1f, header.fractionWithPull(1f), 0f)
        assertEquals(0.5f, header.fractionWithPull(0.5f), 0f)
        // An interrupted dismissal can reopen without losing the shared projection.
        header.finishPull(true)
        assertEquals(1f, header.fractionWithPull(1f), 0f)
        assertEquals(0f, header.fractionWithPull(0f), 0f)
        header.finishPull(false)
        assertEquals(0f, header.fraction, 0f)
        header.beginPull(); header.updateRange(0f); header.finishPull(false)
        assertEquals(0f, header.offset, 0f)
        assertEquals(0f, header.fractionWithPull(1f), 0f)
    }

    @Test fun `overflow scrolling keeps its real offset when Apps opens and closes`() {
        val header = HomeHeaderScroll()
        header.updateRange(100f); header.consume(-100f)
        header.finishPull(true)
        header.finishPull(false)
        assertEquals(100f, header.offset, 0f)
        assertEquals(1f, header.fractionWithPull(0f), 0f)
    }
}
