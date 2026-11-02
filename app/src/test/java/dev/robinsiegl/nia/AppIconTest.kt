package dev.robinsiegl.nia

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29, 35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppIconTest {
    private fun appIcon(): AdaptiveIconDrawable {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val icon = context.getDrawable(R.mipmap.ic_nia)
        assertTrue("The app resource must supply Android's adaptive icon", icon is AdaptiveIconDrawable)
        return icon as AdaptiveIconDrawable
    }

    private fun render(drawable: Drawable, size: Int): Bitmap =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also {
            drawable.setBounds(0, 0, size, size)
            drawable.draw(Canvas(it))
        }

    private fun ink(bitmap: Bitmap): Set<Int> = buildSet {
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width)
            if (Color.alpha(bitmap.getPixel(x, y)) > 128) add(y * bitmap.width + x)
    }

    private fun components(bitmap: Bitmap): Int {
        val remaining = ink(bitmap).toMutableSet()
        var count = 0
        while (remaining.isNotEmpty()) {
            count++
            val queue = ArrayDeque<Int>()
            val start = remaining.first()
            remaining.remove(start)
            queue.add(start)
            while (queue.isNotEmpty()) {
                val index = queue.removeFirst()
                val x = index % bitmap.width
                val y = index / bitmap.width
                for ((nx, ny) in listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1)) {
                    if (nx in 0 until bitmap.width && ny in 0 until bitmap.height) {
                        val next = ny * bitmap.width + nx
                        if (remaining.remove(next)) queue.add(next)
                    }
                }
            }
        }
        return count
    }

    @Test fun `app icon preserves separate foreground forms inside the adaptive safe area`() {
        val icon = appIcon()
        val output = File("build/reports/screenshots/app-icon-api-${Build.VERSION.SDK_INT}.png")
            .apply { parentFile?.mkdirs() }
        output.outputStream().use { render(icon, 512).compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertEquals(Color.BLACK, render(icon.background, 108).getPixel(54, 54))

        val foreground = render(icon.foreground, 108)
        assertEquals(2, components(foreground))
        for (index in ink(foreground)) {
            val dx = index % 108 + .5 - 54
            val dy = index / 108 + .5 - 54
            assertTrue("Foreground must survive the central 66dp safe circle", dx * dx + dy * dy <= 33 * 33)
        }
        for (size in listOf(24, 32, 48))
            assertEquals("Two forms must remain separated at $size px", 2, components(render(icon.foreground, size)))
    }

    @Test @Config(sdk = [35])
    fun `themed icon supplies the same transparent silhouette as the regular foreground`() {
        val icon = appIcon()
        assertNotNull(icon.monochrome)
        val normal = render(icon.foreground, 108)
        val themed = render(requireNotNull(icon.monochrome), 108)
        assertEquals(ink(normal), ink(themed))
        assertEquals(2, components(themed))
    }
}
