package dev.robinsiegl.nia

import dev.robinsiegl.nia.model.*
import org.junit.Assert.*
import org.junit.Test

class AppsPullTest {
    @Test fun `hysteresis permits deliberate reversal without repeated vibration within one gesture`() {
        val pull = AppsPull()
        val cues = mutableListOf<PullCue>()
        pull.drag(-50f, 100f, cues::add)
        assertFalse(pull.armed)
        pull.drag(-55f, 100f, cues::add)
        assertTrue(pull.armed)
        pull.drag(15f, 100f, cues::add)
        assertTrue(pull.armed)
        pull.drag(25f, 100f, cues::add)
        assertFalse(pull.armed)
        pull.drag(-60f, 100f, cues::add)
        assertTrue(pull.armed)
        assertEquals(listOf(PullCue.START, PullCue.ARMED), cues)
        pull.reset()
        assertEquals(0f, pull.preview, 0f); assertFalse(pull.armed)
        pull.drag(-120f, 100f, cues::add)
        assertEquals(listOf(PullCue.START, PullCue.ARMED, PullCue.START, PullCue.ARMED), cues)
    }
    @Test fun `a downward gesture from rest cannot start opening Apps`() {
        val pull = AppsPull()
        pull.drag(200f, 100f) { fail("A downward drag should not give an Apps cue") }
        assertFalse(pull.active); assertFalse(pull.armed)
    }
}
