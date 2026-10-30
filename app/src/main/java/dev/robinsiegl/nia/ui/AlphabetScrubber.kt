package dev.robinsiegl.nia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.robinsiegl.nia.model.AlphabetSection
import kotlin.math.roundToInt

@Composable
internal fun AlphabetScrubber(
    sections: List<AlphabetSection>, firstVisibleItem: Int, onJump: (Int) -> Unit, modifier: Modifier = Modifier,
) {
    val current = sections.indexOfLast { it.index <= firstVisibleItem }.coerceAtLeast(0)
    var active by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    var selected by remember(sections) { mutableIntStateOf(current) }
    val shown = if (active || focused) selected else current
    fun jump(index: Int) {
        selected = index.coerceIn(sections.indices)
        onJump(sections[selected].index)
    }
    Box(modifier.width(96.dp).fillMaxHeight()) {
        if (active || focused) Box(Modifier.align(Alignment.CenterStart).size(44.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
            Text(sections[shown].label, color = MaterialTheme.colorScheme.onPrimary, fontSize = 24.sp)
        }
        Box(Modifier.align(Alignment.CenterEnd).width(48.dp).fillMaxHeight().testTag("alphabet-scrubber")
            .semantics {
                contentDescription = "Alphabet navigation"
                stateDescription = sections[shown].label
                progressBarRangeInfo = ProgressBarRangeInfo(shown.toFloat(), 0f..sections.lastIndex.toFloat(),
                    (sections.size - 2).coerceAtLeast(0))
                setProgress { value ->
                    if (!value.isFinite()) false else {
                        val rounded = value.roundToInt()
                        // Services can request fractional steps even with just two letters.
                        val target = if (rounded == shown && value != shown.toFloat()) {
                            shown + if (value > shown) 1 else -1
                        } else rounded
                        jump(target)
                        true
                    }
                }
            }.onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) false else when (event.key) {
                    Key.DirectionDown -> { jump(shown + 1); true }
                    Key.DirectionUp -> { jump(shown - 1); true }
                    else -> false
                }
            }.onFocusChanged { focused = it.isFocused }.focusable()
            .dragToSelect({ position, size ->
                active = true
                val fraction = (position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                jump((fraction * sections.size).toInt())
            }, { active = false }), contentAlignment = Alignment.Center) {
            if (active || focused) BoxWithConstraints(Modifier.fillMaxHeight().padding(vertical = 4.dp)) {
                val showAll = maxHeight >= with(LocalDensity.current) { (sections.size * 14).sp.toDp() }
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    sections.forEachIndexed { index, section ->
                        if (showAll || index == 0 || index == sections.lastIndex || index == shown) {
                            Text(section.label, fontSize = 10.sp, lineHeight = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (index == shown) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.clearAndSetSemantics {})
                        }
                    }
                }
            } else Box(Modifier.width(2.dp).height(48.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), CircleShape))
        }
    }
}
