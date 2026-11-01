package dev.robinsiegl.nia

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import dev.robinsiegl.nia.model.ShortcutIcon
import dev.robinsiegl.nia.ui.drawable
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MaterialSymbolResourcesTest {
    @Test fun `every bundled symbol renders visible pixels with the shared foreground tint`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val ink = 0xFF68206F.toInt()
        ShortcutIcon.entries.forEach { icon ->
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            val drawable = requireNotNull(context.getDrawable(icon.drawable)).mutate()
            drawable.setBounds(0, 0, 64, 64)
            drawable.setTint(ink)
            drawable.draw(Canvas(bitmap))
            val pixels = IntArray(64 * 64)
            bitmap.getPixels(pixels, 0, 64, 0, 0, 64, 64)
            assertTrue("${icon.name} did not render any tinted pixels", pixels.any { it == ink })
            bitmap.recycle()
        }
    }
}
