package dev.robinsiegl.nia

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.MotionDurationScale
import dev.robinsiegl.nia.ui.DrawerMotion
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MotionClockTest {
    @Test fun `Android zero animation scale finishes the drawer in a single frame`() {
        var frames = 0
        val clock = object : MonotonicFrameClock {
            override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(++frames * 16_000_000L)
        }
        val scale = object : MotionDurationScale { override val scaleFactor = 0f }
        runBlocking(clock + scale) {
            val state = DrawerMotion(false, this)
            var open = false
            state.moveTo(true, false) { open = it }
            yield()
            assertTrue(open)
            assertEquals(1f, state.progress, 0f)
            assertFalse(state.settling)
            assertEquals(1, frames)
        }
    }
}
