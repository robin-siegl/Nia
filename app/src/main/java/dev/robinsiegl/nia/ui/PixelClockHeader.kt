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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import dev.robinsiegl.nia.R
import dev.robinsiegl.nia.model.HomeTextSize
import dev.robinsiegl.nia.model.PixelHeaderLayout
import kotlin.math.max
import kotlin.math.roundToInt

internal data class HeaderText(val text: String, val paint: Paint, val ink: Rect, val left: Float, val top: Float)
internal data class ClockHeaderData(
    val clock: HeaderText, val date: HeaderText, val expandedHeight: Float, val compactHeight: Float,
    val compactClockHeight: Float, val compactDateHeight: Float, val compactClockTop: Float, val compactDateTop: Float,
    val compactLeft: Float, val pixel: Boolean,
)

private fun headerText(text: String, sample: String, typeface: Typeface, height: Float, sampleWidth: Float,
    left: Float, top: Float, availableWidth: Float, color: Int): HeaderText {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        this.typeface = typeface; this.color = color; textSize = 100f
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

private fun classicText(text: String, font: Typeface, fontSize: Float, left: Float, top: Float, width: Float, color: Int): HeaderText {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply { typeface = font; textSize = fontSize; this.color = color }
    val ink = Rect()
    paint.getTextBounds(text, 0, text.length, ink)
    if (ink.width() > width) { paint.textSize *= width / ink.width(); paint.getTextBounds(text, 0, text.length, ink) }
    return HeaderText(text, paint, ink, left, top)
}

/** The same two text paths are translated and scaled throughout a scroll; neither is crossfaded. */
@Composable
internal fun rememberClockHeader(time: String, date: String, width: Float, pixel: Boolean,
    textSize: HomeTextSize, contentTopInset: Dp): ClockHeaderData {
    val context = LocalContext.current
    val density = LocalDensity.current
    val regular = remember(context, pixel) { if (pixel) requireNotNull(ResourcesCompat.getFont(context, R.font.nia_header_regular)) else Typeface.create("sans-serif-light", Typeface.NORMAL) }
    val semibold = remember(context, pixel) { if (pixel) requireNotNull(ResourcesCompat.getFont(context, R.font.nia_header_semibold)) else Typeface.DEFAULT }
    val foreground = MaterialTheme.colorScheme.onSurface.toArgb()
    val oled = pixel && MaterialTheme.colorScheme.background.toArgb() == 0xFF000000.toInt() && foreground == 0xFFE6E6E6.toInt()
    val clockColor = if (oled) 0xFFE2E2E2.toInt() else foreground
    val dateColor = if (oled) 0xFFFFFFFF.toInt() else foreground
    return remember(time, date, width, pixel, textSize, contentTopInset, density, regular, semibold, clockColor, dateColor) {
        with(density) {
            val minimum = 48.dp.toPx()
            val left = 28.dp.toPx()
            val available = (width - left * 2).coerceAtLeast(1f)
            val clock: HeaderText
            val dateLine: HeaderText
            val expanded: Float
            if (pixel) {
                val header = PixelHeaderLayout.forWidth(width, contentTopInset.toPx(), minimum)
                clock = headerText(time.replace(':', ' '), "14 03", regular, header.clockHeight, header.clockSampleWidth,
                    header.clockLeft, header.clockTop, width - header.clockLeft * 2, clockColor)
                dateLine = headerText(date, "Fri, 30 Oct", semibold, header.dateHeight, header.dateSampleWidth,
                    header.dateLeft, header.dateTop, width - header.dateLeft * 2, dateColor)
                expanded = header.height
            } else {
                val c = classicText(time, regular, (56 * textSize.scale).sp.toPx(), left, 0f, available, clockColor)
                val d = classicText(date, semibold, (16 * textSize.scale).sp.toPx(), left, 0f, available, dateColor)
                val clockRow = max(64.dp.toPx(), c.ink.height().toFloat())
                val dateRow = max(minimum, d.ink.height().toFloat())
                clock = c.copy(top = 24.dp.toPx() + (clockRow - c.ink.height()) / 2)
                dateLine = d.copy(top = 24.dp.toPx() + clockRow + (dateRow - d.ink.height()) / 2)
                expanded = 24.dp.toPx() + clockRow + dateRow
            }
            val smallClock = if (pixel) minOf(clock.ink.height().toFloat(), 28.dp.toPx())
                else minOf(clock.ink.height().toFloat(), classicText(time, regular, 26.sp.toPx(), 0f, 0f, available, clockColor).ink.height().toFloat())
            val smallDate = if (pixel) minOf(dateLine.ink.height().toFloat(), 14.dp.toPx())
                else minOf(dateLine.ink.height().toFloat(), classicText(date, semibold, 14.sp.toPx(), 0f, 0f, available, dateColor).ink.height().toFloat())
            val clockRow = max(minimum, smallClock)
            val dateRow = max(minimum, smallDate)
            ClockHeaderData(clock, dateLine, expanded, 12.dp.toPx() + clockRow + dateRow, smallClock, smallDate,
                12.dp.toPx() + (clockRow - smallClock) / 2, 12.dp.toPx() + clockRow + (dateRow - smallDate) / 2, left, pixel)
        }
    }
}

@Composable
internal fun MorphingClockHeader(data: ClockHeaderData, time: String, date: String, collapse: Float,
    onClock: () -> Unit, onDate: () -> Unit, interactive: Boolean, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val fraction = collapse.coerceIn(0f, 1f)
    fun mix(start: Float, end: Float) = start + (end - start) * fraction
    val minimum = with(density) { 48.dp.toPx() }
    val height = mix(data.expandedHeight, data.compactHeight)
    Box(modifier.fillMaxWidth().height(with(density) { height.toDp() }).testTag("home-clock-header")) {
        listOf(data.clock, data.date).forEachIndexed { index, line ->
            val targetHeight = if (index == 0) data.compactClockHeight else data.compactDateHeight
            val scale = mix(1f, targetHeight / line.ink.height().coerceAtLeast(1))
            val x = mix(line.left, data.compactLeft)
            val y = mix(line.top, if (index == 0) data.compactClockTop else data.compactDateTop)
            val inkHeight = line.ink.height() * scale
            val inkWidth = line.ink.width() * scale
            Canvas(Modifier.fillMaxSize()) {
                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    native.save()
                    native.translate(x, y)
                    native.scale(scale, scale)
                    native.drawText(line.text, -line.ink.left.toFloat(), -line.ink.top.toFloat(), line.paint)
                    native.restore()
                }
            }
            val touchHeight = max(minimum, inkHeight)
            Box(Modifier.offset { IntOffset(x.roundToInt(), max(0f, y - (touchHeight - inkHeight) / 2).roundToInt()) }
                .size(with(density) { max(minimum, inkWidth).toDp() }, with(density) { touchHeight.toDp() })
                .testTag(if (index == 0) "home-clock" else "home-date")
                .then(if (interactive) Modifier.clickable(role = Role.Button, onClick = if (index == 0) onClock else onDate) else Modifier)
                .semantics {
                    text = AnnotatedString(line.text)
                    val description = if (index == 0) "Time, $time" else "Date, $date"
                    contentDescription = if (!interactive) description else description + if (index == 0) ". Open Clock" else ". Open Calendar"
                })
        }
    }
}

@Composable
internal fun PixelClockHeader(time: String, date: String, onClock: () -> Unit, onDate: () -> Unit,
    modifier: Modifier = Modifier, contentTopInset: Dp = 0.dp, interactive: Boolean = true) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth().testTag("pixel-clock-header")) {
        val data = rememberClockHeader(time, date, with(density) { maxWidth.toPx() }, true, HomeTextSize.DEFAULT, contentTopInset)
        MorphingClockHeader(data, time, date, 0f, onClock, onDate, interactive)
    }
}
