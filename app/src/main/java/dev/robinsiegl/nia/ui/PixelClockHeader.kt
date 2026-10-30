package dev.robinsiegl.nia.ui

import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.res.ResourcesCompat
import dev.robinsiegl.nia.R
import dev.robinsiegl.nia.model.PixelHeaderLayout
import kotlin.math.max
import kotlin.math.roundToInt

private data class HeaderText(val text: String, val paint: Paint, val ink: Rect, val left: Float, val top: Float) {
    val baseline: Float get() = top - ink.top
    val originX: Float get() = left - ink.left
}

private fun headerText(text: String, sample: String, typeface: Typeface, height: Float, sampleWidth: Float,
    left: Float, top: Float, availableWidth: Float, color: Int): HeaderText {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        this.typeface = typeface
        this.color = color
        textSize = 100f
    }
    val bounds = Rect()
    paint.getTextBounds(sample, 0, sample.length, bounds)
    paint.textSize *= height / bounds.height().coerceAtLeast(1)
    val outline = Path()
    val outlineBounds = RectF()
    paint.getTextPath(sample, 0, sample.length, 0f, 0f, outline)
    outline.computeBounds(outlineBounds, true)
    paint.textScaleX = sampleWidth / outlineBounds.width().coerceAtLeast(1f)
    paint.getTextBounds(text, 0, text.length, bounds)
    if (bounds.width() > availableWidth) {
        paint.textSize *= availableWidth / bounds.width()
        paint.getTextBounds(text, 0, text.length, bounds)
    }
    return HeaderText(text, paint, Rect(bounds), left, top)
}

@Composable
internal fun PixelClockHeader(
    time: String, date: String, onClock: () -> Unit, onDate: () -> Unit,
    modifier: Modifier = Modifier, contentTopInset: Dp = 0.dp, interactive: Boolean = true,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val clockFont = remember(context) { requireNotNull(ResourcesCompat.getFont(context, R.font.nia_header_regular)) }
    val dateFont = remember(context) { requireNotNull(ResourcesCompat.getFont(context, R.font.nia_header_semibold)) }
    val foreground = MaterialTheme.colorScheme.onSurface.toArgb()
    val oledMonochrome = MaterialTheme.colorScheme.background.toArgb() == 0xFF000000.toInt() && foreground == 0xFFE6E6E6.toInt()
    val clockColor = if (oledMonochrome) 0xFFE2E2E2.toInt() else foreground
    val dateColor = if (oledMonochrome) 0xFFFFFFFF.toInt() else foreground
    val displayTime = time.replace(':', ' ')
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = with(density) { maxWidth.toPx() }
        val minimumTouch = with(density) { 48.dp.toPx() }
        val header = PixelHeaderLayout.forWidth(width, with(density) { contentTopInset.toPx() }, minimumTouch)
        val clock = remember(displayTime, width, header, clockFont, clockColor) {
            headerText(displayTime, "14 03", clockFont, header.clockHeight, header.clockSampleWidth,
                header.clockLeft, header.clockTop, width - header.clockLeft * 2, clockColor)
        }
        val dateText = remember(date, width, header, dateFont, dateColor) {
            headerText(date, "Fri, 30 Oct", dateFont, header.dateHeight, header.dateSampleWidth,
                header.dateLeft, header.dateTop, width - header.dateLeft * 2, dateColor)
        }
        Box(Modifier.fillMaxWidth().height(with(density) { header.height.toDp() }).testTag("pixel-clock-header")) {
            Canvas(Modifier.fillMaxSize()) {
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(clock.text, clock.originX, clock.baseline, clock.paint)
                    canvas.nativeCanvas.drawText(dateText.text, dateText.originX, dateText.baseline, dateText.paint)
                }
            }
            listOf(Triple(clock, "Time, $time", onClock), Triple(dateText, "Date, $date", onDate)).forEachIndexed { index, (line, description, action) ->
                val touchHeight = max(minimumTouch, line.ink.height().toFloat())
                val touchTop = max(0f, line.top - (touchHeight - line.ink.height()) / 2f)
                Box(Modifier.offset { IntOffset(line.left.roundToInt(), touchTop.roundToInt()) }
                    .size(with(density) { max(minimumTouch, line.ink.width().toFloat()).toDp() }, with(density) { touchHeight.toDp() })
                    .testTag(if (index == 0) "home-clock" else "home-date")
                    .then(if (interactive) Modifier.clickable(role = Role.Button, onClick = action) else Modifier)
                    .semantics {
                        text = AnnotatedString(line.text)
                        contentDescription = if (!interactive) description else description + if (index == 0) ". Open Clock" else ". Open Calendar"
                    })
            }
        }
    }
}
